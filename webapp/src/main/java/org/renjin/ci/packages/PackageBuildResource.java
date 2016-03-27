package org.renjin.ci.packages;

import com.google.appengine.api.datastore.Entity;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.googlecode.objectify.*;
import com.googlecode.objectify.NotFoundException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.MigrateTestOutput;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;
import org.renjin.ci.packages.results.TestRegressionPage;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.renjin.ci.datastore.PackageBuild.key;

public class PackageBuildResource {

  private static final Logger LOGGER = Logger.getLogger(PackageBuildResource.class.getName());

  private final PackageVersionId packageVersionId;
  private final PackageBuildId buildId;
  private final long buildNumber;


  public PackageBuildResource(PackageVersionId packageVersionId, long buildNumber) {
    this.packageVersionId = packageVersionId;
    this.buildNumber = buildNumber;
    this.buildId = new PackageBuildId(packageVersionId, buildNumber);
  }

  @GET
  @Produces("text/html")
  public Viewable get() throws IOException {

    // Start fetching list of builds
    PackageBuildPage page = ObjectifyService.ofy().transact(new Work<PackageBuildPage>() {
      @Override
      public PackageBuildPage run() {
        return new PackageBuildPage(buildId);
      }
    });

    Map<String, Object> model = new HashMap<>();
    model.put("build", page);

    return new Viewable("/buildResult.ftl", model);
  }
  
  @GET
  @Path("tests/regression/${testName}")
  public Viewable getRegression(@PathParam("testName") String testName) {
    TestRegressionPage regressionPage = TestRegressionPage.query(buildId, testName);
 
    throw new UnsupportedOperationException();
  }
  
  @GET
  @Path("migrateTests")
  public Response migrateTests() {
    MigrateTestOutput mapper = new MigrateTestOutput();
    mapper.beginSlice();
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(buildId);
    for (PackageTestResult result : results) {
      Entity entity = ObjectifyService.ofy().save().toEntity(result);
      mapper.migrateEntity(entity);
      
    }
    return Response.ok("Done").build();
  }

  @POST
  @Consumes("application/json")
  public void postResult(final PackageBuildResult buildResult) {

    LOGGER.info("Received build results for " + packageVersionId + "-b" + buildNumber + ": " + buildResult.getOutcome());

    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        Key<PackageBuild> buildKey = key(packageVersionId, buildNumber);

        // Retrieve the current status of this package version and the build itself
        PackageBuild build;
        try {
          build = ofy().load().key(buildKey).safe();
        } catch (NotFoundException notFoundException) {
          LOGGER.info("Cannot find PackageBuild entity to update: " + buildKey);
          return;
        }

        // Has the status already been reported?
        if (build.getOutcome() != null) {
          LOGGER.log(Level.INFO, "Build " + build.getId() + " is already marked as " + build.getOutcome());

        } else {

          List<Object> toSave = Lists.newArrayList();
          LOGGER.log(Level.INFO, "Marking " + build.getId() + " as " + buildResult.getOutcome());

          build.setOutcome(buildResult.getOutcome());
          build.setEndTime(System.currentTimeMillis());
          build.setDuration(build.getEndTime() - build.getStartTime());
          build.setNativeOutcome(buildResult.getNativeOutcome());
          build.setResolvedDependencies(buildResult.getResolvedDependencies());
          build.setBlockingDependencies(buildResult.getBlockingDependencies());
          toSave.add(build);

          if (buildResult.getTestResults() != null) {
            for (TestResult result : buildResult.getTestResults()) {
              PackageTestResult test = new PackageTestResult(buildKey, result.getName());
              test.setDuration(result.getDuration());
              test.setPassed(result.isPassed());
              test.setOutput(result.getOutput());
              test.setRenjinVersion(build.getRenjinVersion());
              if (result.getDuration() > 0) {
                test.setDuration(result.getDuration());
              }
              toSave.add(test);
            }
          }

          ObjectifyService.ofy().save().entities(toSave);


          maybeUpdateLastSuccessfulBuild(build);

          // Update the delta (regression/progression) flags for this build
          DeltaBuilder.update(packageVersionId, Optional.of(build));

        }
      }
    });
  }




  /**
   *
   * If the given {@code build} was successful, set it as the last successful build 
   * of the corresponding PackageVersion.
   *
   */
  private void maybeUpdateLastSuccessfulBuild(PackageBuild build) {

    if (build.getOutcome() == BuildOutcome.SUCCESS) {
      PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

      LOGGER.log(Level.INFO, "lastSuccessfulBuildNumber was " + packageVersion.getLastSuccessfulBuildNumber());

      if (buildNumber > packageVersion.getLastSuccessfulBuildNumber()) {

        LOGGER.log(Level.INFO, "Setting lastSuccessfulBuildNumber to #" + buildNumber);

        packageVersion.setLastSuccessfulBuildNumber(buildNumber);
        ObjectifyService.ofy().save().entity(packageVersion);

      } else {
        LOGGER.log(Level.INFO, "Last successful build number remains at " + buildNumber);
      }
    }
  }


}
