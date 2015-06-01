
package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.TestCase;
import org.renjin.ci.model.TestResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Specific version of a package
 */
public class PackageVersionResource {
  private final PackageVersion packageVersion;
  private PackageVersionId packageVersionId;

  public PackageVersionResource(PackageVersion packageVersion) {
    this.packageVersionId = packageVersion.getPackageVersionId();
    this.packageVersion = packageVersion;
  }

  @GET
  @Produces("text/html")
  public Viewable getPage() {
    VersionViewModel viewModel = new VersionViewModel(packageVersion);
    viewModel.setBuilds(PackageDatabase.getBuilds(packageVersionId).list());
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
        if (source != null) {
          TestCase testCase = new TestCase();
          testCase.setId(example.getName());
          testCase.setSource(source.getSource());
          cases.add(testCase);
        }
      }
    }
    
    return cases;
  }
  
  @POST
  @Path("examples/results")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response postExampleTestResult(@PathParam("exampleId") String exampleId, TestResult testResult) {

    Key<PackageExample> exampleKey = PackageExample.key(packageVersionId, exampleId);

    PackageBuildId buildId = new PackageBuildId(packageVersionId, testResult.getPackageBuildVersion());
    
    // Verify that the example exists
    PackageExample example = ObjectifyService.ofy().load().key(exampleKey).now();
    if(example == null) {
      return Response
          .status(Response.Status.BAD_REQUEST)
          .entity(String.format("No such example '%s'", exampleId))
          .build();
    }
    
    // Log the results
    PackageExampleResult result = new PackageExampleResult();
    result.setExampleKey(exampleKey);
    result.setPackageBuildNumber(buildId.getBuildNumber());
    result.setRenjinVersion(testResult.getRenjinVersion());
    result.setDuration(testResult.getDuration());
    result.setRunTime(new Date());
    result.setPassed(testResult.isPassed());
    ObjectifyService.ofy().save().entity(result).now();
    
    // Now we need to...
    // (1) Determine whether this test is a regression/progression
    // (2) Update the status of the package version --  
    // (3) Update the 

    return Response.ok().build();
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
    return new DependencyResolution(packageVersion);
  }
}
