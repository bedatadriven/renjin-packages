package org.renjin.ci.qa;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.NoRobots;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/qa")
@NoRobots
public class QaResources {
  
  private static Logger LOGGER = Logger.getLogger(QaResources.class.getName());

  
  @GET
  @Path("dashboard")
  public Viewable getDashboard() {

    Multimap<RenjinVersionId, RenjinVersionStat> statMap = HashMultimap.create();

    RenjinVersionStats stats = PackageDatabase.getRenjinVersionStats();
    if(stats != null) {
      for (RenjinVersionStat stat : stats.getVersions()) {
        statMap.put(stat.getRenjinVersionId(), stat);
      }
    }
    
    List<RenjinVersionSummary> versions = new ArrayList<>();
    for (RenjinVersionId renjinVersionId : statMap.keySet()) {
      versions.add(new RenjinVersionSummary(renjinVersionId, statMap.get(renjinVersionId)));
    }

    Collections.sort(versions, Ordering.natural().reverse());
    
    Map<String, Object> model = new HashMap<>();    
    model.put("versions", versions);
    
    return new Viewable("/dashboard.ftl", model);
  }
  
  @GET
  @Path("gcc-bridge")
  public Viewable getGccBridgeStatus() {
    QueryResultIterable<PackageVersion> versions = ofy().load()
        .type(PackageVersion.class)
        .filter("needsCompilation", true)
        .iterable();

    List<Key<PackageBuild>> buildsToFetch = new ArrayList<>();
    for (PackageVersion version : versions) {
      if(version.getLastBuildNumber() != 0) {
        buildsToFetch.add(PackageBuild.key(version.getPackageVersionId(), version.getLastBuildNumber()));
      }
    }

    Collection<PackageBuild> builds = ObjectifyService.ofy().load().keys(buildsToFetch).values();

    LOGGER.info("Build count = " + builds.size());
    
    Map<String, Object> model = new HashMap<>();
    model.put("builds", builds);
    
    return new Viewable("/gccBridge.ftl", model);
  }
  
  @GET
  @Path("progress/{renjinVersion}")
  public Viewable getDeltas(@PathParam("renjinVersion") String renjinVersion) {
    
    QueryResultIterable<PackageVersionDelta> deltas = ofy().load()
        .type(PackageVersionDelta.class)
        .filter("renjinVersions", renjinVersion)
        .iterable();

    List<DeltaViewModel> packages = new ArrayList<>();
    for (PackageVersionDelta delta : deltas) {
      Optional<BuildDelta> build = delta.getBuild(renjinVersion);
      if(build.isPresent()) {
        packages.add(new DeltaViewModel(delta.getPackageVersionId(), build.get()));
      }
    }
    
    Map<String, Object> model = new HashMap<>();
    model.put("renjinVersion", renjinVersion);
    model.put("packageVersions", packages);
    

    return new Viewable("/versionDeltas.ftl", model);
  }
  
  @GET
  @Path("testRegressions")
  public Viewable getTestRegressions(@QueryParam("renjinVersion") final String renjinVersion) {

    Query<TestRegression> query = PackageDatabase.getOpenTestRegressions();
    if(!Strings.isNullOrEmpty(renjinVersion)) {
      query = query.filter("renjinVersion", renjinVersion);
    }

    Iterable<TestRegression> pending = Iterables.filter(query, new Predicate<TestRegression>() {
      @Override
      public boolean apply(@Nullable TestRegression input) {
        return input.isOpen() && input.getStatus() != TestRegressionStatus.INVALID;
      }
    });

    TestRegressionIndexPage page = new TestRegressionIndexPage(pending);

    Map<String, Object> model = new HashMap<>();
    model.put("page", page);
    
    return new Viewable("/testRegressions.ftl", model);
  }


  @GET
  @Path("testRegressions/unconfirmed")
  public Viewable getUnconfirmedRegressions() {
    QueryResultIterable<TestRegression> unconfirmed = PackageDatabase.getTestRegressions()
        .order("triage")
        .iterable();

    TestRegressionIndexPage page = new TestRegressionIndexPage(unconfirmed);

    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/testRegressions.ftl", model);
  }

  @GET
  @Path("testRegressions/closed")
  public Viewable getClosedTestRegressions() {

    Query<TestRegression> query = PackageDatabase.getTestRegressions()
        .order("-dateClosed");

    TestRegressionIndexPage page = new TestRegressionIndexPage(query);

    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/testRegressionsClosed.ftl", model);
  }



  public static URI findNextRegression(UriInfo uriInfo, Key<PackageTestResult> key) {
    PackageBuildId packageBuildId = PackageTestResult.buildIdOf(key);
    String testName = key.getName();

    return findNextRegression(uriInfo, packageBuildId.getPackageVersionId(), testName);
  }

  public static URI findNextRegression(UriInfo uriInfo, PackageVersionId packageVersionId, String testName) {
    Iterable<PackageVersionDelta> deltas = ofy().load().type(PackageVersionDelta.class)
        .filter("regression", true)
        .filterKey(">=", PackageVersionDelta.key(packageVersionId))
        .chunk(5)
        .iterable();

    for (PackageVersionDelta delta : deltas) {
      List<String> regressions = Lists.newArrayList(delta.getTestRegressions());
      Collections.sort(regressions);
      for (String regressionName : regressions) {
        if (delta.getPackageVersionId().compareTo(packageVersionId) > 0 ||
            regressionName.compareTo(testName) > 0) {

        }
      }
    }

    // If this is the last test, then
    // Redirect to history page
    return uriInfo.getBaseUriBuilder()
        .path("qa")
        .path("testRegressions")
        .build();
  }

  @Path("testRegression/{groupId}/{packageName}/{packageVersion}/{testName}/{buildNumber}")
  public TestRegressionResource getTestRegression(@PathParam("groupId") String groupId,
                                                  @PathParam("packageName") String packageName,
                                                  @PathParam("packageVersion") String packageVersion,
                                                  @PathParam("testName") final String testName,
                                                  @PathParam("buildNumber") final long buildNumber) {


    final PackageVersionId pvid = new PackageVersionId(groupId, packageName, packageVersion);
    TestRegression regression = ObjectifyService.ofy().transactNew(new Work<TestRegression>() {
      @Override
      public TestRegression run() {
        return PackageDatabase.getTestRegression(new PackageBuildId(pvid, buildNumber), testName).now();
      }
    });

    if(regression == null) {
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("No such regression").build());
    }

    return new TestRegressionResource(regression);
  }
  
  @GET
  @Path("stillFailing")
  public String getMostRecentFailingBuild(@QueryParam("pv") String packageVersion, 
                                          @QueryParam("renjinVersion") String renjinVersion,
                                          @QueryParam("test") String testName) {
    PackageVersionId packageVersionId = PackageVersionId.fromTriplet(packageVersion);
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersionId);
   
    // The version of Renjin with the first failure
    RenjinVersionId regressionVersion = RenjinVersionId.valueOf(renjinVersion);
    
    // The last version of Renjin still failing
    RenjinVersionId lastFailingVersion = regressionVersion;
    long lastFailingBuild = 0;
    for (PackageTestResult result : results) {
      if(result.getRenjinVersionId().compareTo(lastFailingVersion) > 0 && !result.isPassed()) {
        lastFailingVersion = result.getRenjinVersionId();
        lastFailingBuild = result.getPackageBuildNumber();
      }
    }
    if(lastFailingVersion.equals(regressionVersion)) {
      return "";
    } else {
      return "Still failing on " + lastFailingVersion + " <a href=\"" + 
          new PackageBuildId(packageVersionId, lastFailingBuild).getPath() + "\">#" + lastFailingBuild + "</a>";
    }
  }


  @GET
  @Produces("text/html")
  @Path("markTestResults")
  public Viewable getMarkTestForm(@Context UriInfo uriInfo, @QueryParam("packageId") String packageId, @QueryParam("testName") String testName)
      throws URISyntaxException {

    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserLoggedIn()) {
      String loginUrl = userService.createLoginURL(uriInfo.getRequestUri().toString());
      throw new WebApplicationException(Response.seeOther(new URI(loginUrl)).build());
    }
    if(!userService.isUserAdmin()) {
      throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
    }


    Map<String, Object> model = new HashMap<>();
    model.put("page", new MarkTestsPage(PackageId.valueOf(packageId), testName));

    return new Viewable("/testMark.ftl", model);
  }


  @POST
  @Path("updateTestResults")
  @Consumes("application/x-www-form-urlencoded")
  public Response post(@Context UriInfo uriInfo, MultivaluedMap<String, String> params) {

    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserAdmin()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    final String reason = params.getFirst("reason");
    if(com.google.common.base.Strings.isNullOrEmpty(reason)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Must provide a reason").build();
    }

    Set<PackageVersionId> packageVersionIds = Sets.newHashSet();
    
    final List<Key<PackageTestResult>> toUpdate = Lists.newArrayList();
    for (String paramName : params.keySet()) {
      if(paramName.startsWith("result-") && "true".equals(params.getFirst(paramName))) {
        String webSafeKey = paramName.substring("result-".length());
        Key<PackageTestResult> resultKey = Key.create(webSafeKey);
        Key<PackageBuild> buildKey = resultKey.getParent();
        Key<PackageVersion> versionKey = buildKey.getParent();
        
        packageVersionIds.add(PackageVersion.idOf(versionKey));
        toUpdate.add(resultKey);
      }
    }

    // Update the entities
    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        Collection<PackageTestResult> results = ObjectifyService.ofy().load().keys(toUpdate).values();
        for (PackageTestResult result : results) {
          result.setPassed(false);
          result.setManualFail(true);
          result.setManualFailReason(reason);
        }
        ObjectifyService.ofy().save().entities(results);
      }
    });

    // Schedule recalculate deltas
    for (PackageVersionId packageVersionId : packageVersionIds) {
      QueueFactory.getDefaultQueue().add(TaskOptions.Builder
          .withUrl(packageVersionId.getPath() + "/updateDeltas")
          .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3)));
    }


    return Response.seeOther(findNextRegression(uriInfo, toUpdate.get(0))).build();
  }

}
