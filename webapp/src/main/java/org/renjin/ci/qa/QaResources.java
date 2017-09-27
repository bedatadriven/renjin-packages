package org.renjin.ci.qa;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.packages.DeltaBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/qa")
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

    Iterable<PackageVersionDelta> deltas = ofy().load().type(PackageVersionDelta.class)
        .filter("regression", true)
        .iterable();

    Predicate<TestRegressionEntry> filter = Predicates.alwaysTrue();
    if(!Strings.isNullOrEmpty(renjinVersion)) {
      filter = new Predicate<TestRegressionEntry>() {
        @Override
        public boolean apply(TestRegressionEntry input) {
          return input.getBrokenRenjinVersionId().equals(RenjinVersionId.valueOf(renjinVersion));
        }
      };
    }
    
    TestRegressionsPage page = new TestRegressionsPage(deltas, filter);
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", page);
    
    return new Viewable("/testRegressions.ftl", model);
  }

  @Path("testRegression/{groupId}/{packageName}/{packageVersion}/{testName}")
  public TestRegressionResource getTestRegression(@PathParam("groupId") String groupId,
                                                  @PathParam("packageName") String packageName,
                                                  @PathParam("packageVersion") String packageVersion,
                                                  @PathParam("testName") String testName) {

    PackageVersionId pvid = new PackageVersionId(groupId, packageName, packageVersion);
    return new TestRegressionResource(pvid, testName);
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

    // Recalculate deltas
    for (PackageVersionId packageVersionId : packageVersionIds) {
      DeltaBuilder.update(packageVersionId, Optional.<PackageBuild>absent(), Collections.<PackageTestResult>emptyList());
    }

    // Redirect to history page
    UriBuilder historyUri = uriInfo.getBaseUriBuilder()
        .path("qa")
        .path("testRegressions");

    return Response.seeOther(historyUri.build()).build();

  }
}
