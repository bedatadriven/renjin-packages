
package org.renjin.ci.packages;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.objectify.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.TestCase;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specific version of a package
 */
public class PackageVersionResource {
  
  private static final Logger LOGGER = Logger.getLogger(PackageVersionResource.class.getName());
  
  private final PackageVersion packageVersion;
  private PackageVersionId packageVersionId;

  public PackageVersionResource(PackageVersion packageVersion) {
    this.packageVersionId = packageVersion.getPackageVersionId();
    this.packageVersion = packageVersion;
  }

  @GET
  @Produces("text/html")
  public Viewable getPage() {
    PackageVersionPage viewModel = new PackageVersionPage(packageVersion);
  
    Map<String, Object> model = new HashMap<>();
    model.put("version", viewModel);

    return new Viewable("/packageVersion.ftl", model);
  }

  @Path("build/{buildNumber}")
  public PackageBuildResource getBuild(@PathParam("buildNumber") int buildNumber) {
    return new PackageBuildResource(packageVersion.getPackageVersionId(), buildNumber);
  }

  @GET
  @Path("lastSuccessfulBuild")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLastSuccessfulBuild() {
    if(packageVersion.hasSuccessfulBuild()) {
      return Response.ok().entity(packageVersion.getLastSuccessfulBuildVersion()).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Allocate a new build number for this package version
   */
  @POST
  @Path("builds")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public PackageBuild startBuild(@FormParam("renjinVersion") final String renjinVersion) {
    
    if(Strings.isNullOrEmpty(renjinVersion)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
    return ObjectifyService.ofy().transactNew(new Work<PackageBuild>() {
      @Override
      public PackageBuild run() {
        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

        // increment next build number
        long nextBuild = packageVersion.getLastBuildNumber() + 1;
        packageVersion.setLastBuildNumber(nextBuild);

        PackageBuild packageBuild = new PackageBuild(packageVersionId, nextBuild);
        packageBuild.setRenjinVersion(renjinVersion);
        packageBuild.setStartTime(new Date().getTime());

        ObjectifyService.ofy().save().entities(packageBuild, packageVersion);

        return packageBuild;
      }
    });
  }

  @GET
  @Path("examples")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TestCase> getExamples() {
    List<PackageExample> examples = ObjectifyService.ofy().load().type(PackageExample.class)
        .ancestor(PackageVersion.key(packageVersionId))
        .chunk(1000)
        .list();

    if(examples.isEmpty()) {
      examples = ExamplesExtractor.extract(packageVersionId);
    }
    
    List<Key<PackageExampleSource>> sources = new ArrayList<>();
    for (PackageExample example : examples) {
      if(example.getSource() != null) {
        sources.add(example.getSource().getKey());
      }
    }

    Map<Key<PackageExampleSource>, PackageExampleSource> sourceMap = ObjectifyService.ofy().load().keys(sources);
    
    List<TestCase> cases = new ArrayList<>();
    for (PackageExample example : examples) {
      if(example.getSource() != null) {
        PackageExampleSource source = sourceMap.get(example.getSource().getKey());
        if (source != null && !Strings.isNullOrEmpty(source.getSource())) {
          TestCase testCase = new TestCase();
          testCase.setId(example.getName());
          testCase.setSource(source.getSource());
          cases.add(testCase);
        }
      }
    }
    
    return cases;
  }
  
  @GET
  @Path("builds")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageBuild> getBuilds() {
    return Lists.newArrayList(PackageDatabase.getBuilds(packageVersionId));
  }
  
  @GET
  @Path("updateDeltas")
  @Produces(MediaType.TEXT_PLAIN)
  public String updateDeltas() {
    DeltaBuilder.update(packageVersionId, Optional.<PackageBuild>absent(), Collections.<PackageTestResult>emptyList());
    return "Done.";
  }
  
  @GET
  @Path("check")
  public Response check() {
    ReComputeBuildDeltas markBuildDeltas = new ReComputeBuildDeltas();
    markBuildDeltas.map(PackageVersion.key(packageVersionId).getRaw());

    return Response.ok("Done").build();
  }
  
  @Path("resolveDependencies")
  public DependencyResolution resolveDependencies() {
    try {
      return new DependencyResolution(packageVersion);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception thrown while resolving dependencies: " + e.getMessage(), e);
      throw e;
    }
  }

  @GET
  @Produces("text/html")
  @Path("source")
  public Viewable getSourceIndex() {
    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("files", Lists.newArrayList(PackageDatabase.getPackageSourceKeys(packageVersionId)));
    
    return new Viewable("/packageSourceIndex.ftl", model);
  }

  @GET
  @Produces("text/html")
  @Path("source/{file:.+}")
  public Viewable getSourceFile(@PathParam("file") String filename) {

    LoadResult<PackageSource> source = PackageDatabase.getSource(packageVersionId, filename);

    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("filename", filename);
    model.put("lines", source.safe().parseLines());
    
    return new Viewable("/packageSource.ftl", model);
  }
  
  @GET
  @Produces("text/html")
  @Path("compareDependencies")
  public Viewable compareDependencies(@QueryParam("fromBuild") long fromBuild, @QueryParam("toBuild") long toBuild) {
    throw new UnsupportedOperationException();
  }
  
  @GET
  @Produces("text/html")
  @Path("test/{testName}/history")
  public Viewable getTestHistory(@PathParam("testName") String testName) {
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", new TestHistoryPage(packageVersion, testName));
    
    return new Viewable("/testHistory.ftl", model);
  }
  
  @GET
  @Produces("text/html")
  @Path("test/{testName}/mark")
  public Viewable getMarkTestForm(@Context UriInfo uriInfo, @PathParam("testName") String testName)
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
    model.put("page", new TestHistoryPage(packageVersion, testName));

    return new Viewable("/testMark.ftl", model);
  }
  
  @POST
  @Path("test/{testName}/mark")
  @Consumes("application/x-www-form-urlencoded")
  public Response post(@PathParam("testName") String testName, @Context UriInfo uriInfo,
                       MultivaluedMap<String, String> params) {

    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserAdmin()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    final String reason = params.getFirst("reason");
    if(Strings.isNullOrEmpty(reason)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Must provide a reason").build();
    }

    final List<Key<PackageTestResult>> toUpdate = Lists.newArrayList();
    for (String paramNames : params.keySet()) {
      if(paramNames.startsWith("b")) {
        long buildNumber = Long.parseLong(paramNames.substring(1));
        PackageBuildId buildId = new PackageBuildId(packageVersionId, buildNumber);
        toUpdate.add(PackageTestResult.key(buildId, testName));
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
    DeltaBuilder.update(packageVersionId, Optional.<PackageBuild>absent(), Collections.<PackageTestResult>emptyList());

    // Redirect to history page
    UriBuilder historyUri = uriInfo.getBaseUriBuilder()
        .path(packageVersion.getPath())
        .path("test")
        .path(testName)
        .path("history");

    return Response.seeOther(historyUri.build()).build();

  }
}
