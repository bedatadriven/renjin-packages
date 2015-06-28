
package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.TestCase;
import org.renjin.ci.model.TestResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
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
  @Path("examples/run/{runNumber}")
  public Viewable getExampleRunResults(@PathParam("runNumber") int testRunNumber) {
    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("run", PackageDatabase.getExampleRun(packageVersionId, testRunNumber).safe());
    model.put("results", Lists.newArrayList(PackageDatabase.getExampleResults(packageVersionId, testRunNumber)));
    
    return new Viewable("/exampleRun.ftl", model);
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
  
  @POST
  @Path("examples/results")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response postExampleTestResult(List<TestResult> testResults) {

    // Get the next sequence number
    long runNumber = ObjectifyService.ofy().transact(new Work<Long>() {
      @Override
      public Long run() {
        PackageVersion pv = PackageDatabase.getPackageVersion(packageVersionId).get();
        long lastExampleRun = pv.getLastExampleRun();
        long exampleRun = lastExampleRun + 1;
        pv.setLastExampleRun(exampleRun);
        ObjectifyService.ofy().save().entity(pv);
        return exampleRun;
      }
    });

    // Log the results
    
    List<Object> toSave = new ArrayList<>();

    PackageExampleRun run = new PackageExampleRun(packageVersion, runNumber);
    run.setRenjinVersion(testResults.get(0).getRenjinVersion());
    toSave.add(run);
    
    for (TestResult testResult : testResults) {
      
      LOGGER.info("Recording results for " + testResult.getId());

      PackageBuildId buildId = new PackageBuildId(packageVersionId, testResult.getPackageBuildVersion());
      PackageExampleResult result = new PackageExampleResult(run, testResult.getId());

      // Verify that the example exists
      PackageExample example = ObjectifyService.ofy().load().key(result.getExampleKey()).now();
      if(example == null) {
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(String.format("No such example '%s'", testResult.getId()))
            .build();
      }
      
      TestOutput output = new TestOutput(testResult.getOutput());
      toSave.add(output);
      
      result.setPackageBuildNumber(buildId.getBuildNumber());
      result.setRenjinVersion(testResult.getRenjinVersion());
      result.setDuration(testResult.getDuration());
      result.setRunTime(new Date());
      result.setPassed(testResult.isPassed());
      result.setOutputKey(output.getSha1());
      toSave.add(result);
    }
    
    ObjectifyService.ofy().save().entities(toSave).now();
    
    LOGGER.info("Saved " + toSave.size() + " entities.");
    
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
