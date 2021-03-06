package org.renjin.ci.packages;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.NoRobots;
import org.renjin.ci.admin.migrate.RecomputeBuildGrades;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.lang.String.format;
import static org.renjin.ci.datastore.PackageBuild.key;

@NoRobots
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
  public Viewable get(@QueryParam("builds") String builds) throws IOException {
    Map<String, Object> model = new HashMap<>();
    model.put("build", new PackageBuildPage(buildId));

    return new Viewable("/buildResult.ftl", model);
  }
  
  @GET
  @Produces("text/x-shellscript")
  @Path("rebuild.sh")
  public String getRebuildScript() {
    PackageVersionId versionId = buildId.getPackageVersionId();

    String dirName = versionId.getPackageName() + "_" + buildId.getBuildVersion();
    String archiveName = versionId.getPackageName() + "_" + versionId.getVersionString() + ".tar.gz";
    String sourceUrl = format("https://storage.googleapis.com/renjinci-package-sources/%s/%s",
        versionId.getGroupId(),
        archiveName);

    StringBuilder script = new StringBuilder();
    script.append(format("mkdir %s\n", dirName));
    script.append(format("cd %s\n", dirName));
    script.append(format("curl %s | tar -xz --strip-components 1\n", sourceUrl));
    script.append(format("curl %s > pom.xml\n", buildId.getRepoPomLink()));
    script.append(format("echo Build directory created in %s\n", dirName));
    script.append(format("echo To build: cd %s && mvn clean install\n", dirName));
    return script.toString();
  }

  @GET
  @Path("testResults")
  @Produces("application/json")
  public List<TestResult> getTestResults() {
    List<TestResult> results = new ArrayList<>();
    for (PackageTestResult testResult : PackageDatabase.getTestResults(buildId)) {
      results.add(testResult.toTestResult());
    }

    return results;
  }

  @POST
  @Consumes("application/json")
  public Response postResult(final PackageBuildResult buildResult) {

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

          List<PackageTestResult> testResults = Lists.newArrayList();
          if (buildResult.getTestResults() != null) {
            for (TestResult result : buildResult.getTestResults()) {
              PackageTestResult test = new PackageTestResult(buildKey, result.getName());
              test.setDuration(result.getDuration());
              test.setPassed(result.isPassed());
              test.setRenjinVersion(build.getRenjinVersion());
              test.setTestType(result.getTestType());
              test.setOutput(result.isOutput());
              test.setFailureMessage(result.getFailureMessage());
              if (result.getDuration() > 0) {
                test.setDuration(result.getDuration());
              }
              testResults.add(test);
              toSave.add(test);
            }
          }

          build.setPatchId(buildResult.getPatchId());
          build.setOutcome(buildResult.getOutcome());
          build.setEndTime(System.currentTimeMillis());
          build.setDuration(build.getEndTime() - build.getStartTime());
          build.setNativeOutcome(buildResult.getNativeOutcome());
          build.setResolvedDependencies(buildResult.getResolvedDependencies());
          build.setBlockingDependencies(buildResult.getBlockingDependencies());

          build.setGrade(RecomputeBuildGrades.computeGrade(
                  buildResult.getOutcome(),
                  buildResult.getNativeOutcome(),
                  testResults));

          toSave.add(build);

          maybeUpdateBestGrade(build, toSave);

          ObjectifyService.ofy().save().entities(toSave);

          maybeUpdateLastSuccessfulBuild(build);
        }
      }


    });

    // Update the delta (regression/progression) flags for this build
    if(buildResult.getOutcome() != BuildOutcome.BLOCKED) {
      scheduleDeltaUpdate(packageVersionId);
    }

    return Response.ok().build();
  }

  private void scheduleDeltaUpdate(PackageVersionId packageVersionId) {
    QueueFactory.getDefaultQueue().add(
        TaskOptions.Builder.withUrl(packageVersionId.getPath() + "/updateDeltas")
            .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3))
            .method(TaskOptions.Method.POST));
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


  private void maybeUpdateBestGrade(PackageBuild build, List<Object> toSave) {
    if(build.getOutcome() == BuildOutcome.SUCCESS && build.getGradeInteger() > 0) {
      org.renjin.ci.datastore.Package packageEntity = PackageDatabase.getPackageOf(build.getPackageVersionId());
      if(packageEntity.getGrade() == null ||
          build.getGradeInteger() > packageEntity.getGradeInteger() ||
          (build.getGradeInteger() == packageEntity.getGradeInteger() &&
           build.getPackageVersionId().isNewer(packageEntity.getBestPackageVersionId()))) {

        packageEntity.setGrade(build.getGrade());
        packageEntity.setBestVersion(build.getPackageVersionId());

        toSave.add(packageEntity);
      }
    }
  }

}
