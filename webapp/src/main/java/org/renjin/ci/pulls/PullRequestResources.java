package org.renjin.ci.pulls;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("pull")
public class PullRequestResources {

  private static final Logger LOGGER = Logger.getLogger(PullRequestResources.class.getName());


  @GET
  @Produces("text/html")
  @Path("{number}")
  public Viewable getPage(@PathParam("number") final int number) throws IOException {


    PullRequestPage page = ObjectifyService.ofy().transact(new Work<PullRequestPage>() {
      @Override
      public PullRequestPage run() {
        return new PullRequestPage(number);
      }
    });



    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/pullRequest.ftl", model);
  }

  @GET
  @Produces("text/html")
  @Path("{pullNumber}/build/{pullBuildNumber}/package/{group}/{packageName}/{packageVersion}")
  public Viewable getPackageBuildPage(
      @PathParam("pullNumber") int pullNumber,
      @PathParam("pullBuildNumber") long pullBuildNumber,
      @PathParam("group") String groupId,
      @PathParam("packageName") String packageName,
      @PathParam("packageVersion") String packageVersion) {

    final PullBuildId pullBuildId = new PullBuildId(pullNumber, pullBuildNumber);
    final PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, packageVersion);

    PullPackageBuildPage page = ObjectifyService.ofy().transact(new Work<PullPackageBuildPage>() {
      @Override
      public PullPackageBuildPage run() {
        return new PullPackageBuildPage(pullBuildId, packageVersionId);
      }
    });

    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/pullPackageBuildResult.ftl", model);
  }


  @POST
  @Path("{pullNumber}/build/{pullBuildNumber}/packageBuild")
  @Consumes("application/json")
  public void postPackageBuildResult(
      @PathParam("pullNumber") long pullNumber,
      @PathParam("pullBuildNumber") long pullBuildNumber,
      PackageBuildResult result) {

    PackageVersionId pvid = PackageVersionId.fromTriplet(result.getPackageVersionId());

    LOGGER.info("Storing results for PR#" + pullNumber + ", Build " + pullBuildNumber + ", " + pvid);

    Optional<PackageBuild> releaseBuild = findReleaseBuild(pvid);

    Map<String, PackageTestResult> testMap = queryTestResults(releaseBuild);

    PullPackageBuild build = new PullPackageBuild(pullNumber, pullBuildNumber, pvid);

    build.setOutcome(result.getOutcome());
    build.setNativeOutcome(result.getNativeOutcome());
    build.setTimestamp(System.currentTimeMillis());

    // Compare to release build
    if(releaseBuild.isPresent()) {
      LOGGER.info("Comparing with build " + releaseBuild.get().getId());
      build.setReleaseBuild(releaseBuild.get());
    } else {
      LOGGER.info("No release build available for comparison");
    }


    List<PullTestResult> testResults = new ArrayList<>();
    for (TestResult testResult : result.getTestResults()) {
      PullTestResult ptr = new PullTestResult(Key.create(build), testResult.getName());
      ptr.setPassed(testResult.isPassed());
      ptr.setTestType(testResult.getTestType());
      ptr.setOutput(testResult.isOutput());
      ptr.setDuration(testResult.getDuration());
      ptr.setFailureMessage(testResult.getFailureMessage());
      ptr.setTimestamp(System.currentTimeMillis());
      ptr.setReleaseResult(testMap.get(testResult.getName()));

      testResults.add(ptr);
    }

    build.setTestRegressionCount(countRegressions(testResults));
    build.setTestProgressionCount(countProgressions(testResults));

    final List<Object> toSave = new ArrayList<>();
    toSave.add(build);
    toSave.addAll(testResults);

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        PackageDatabase.save(toSave).now();
      }
    });

  }

  private int countRegressions(List<PullTestResult> testResults) {
    int count = 0;
    for (PullTestResult testResult : testResults) {
      if(testResult.isRegression()) {
        count++;
      }
    }
    return count;
  }


  private int countProgressions(List<PullTestResult> testResults) {
    int count = 0;
    for (PullTestResult testResult : testResults) {
      if(testResult.isProgression()) {
        count++;
      }
    }
    return count;
  }


  private Map<String, PackageTestResult> queryTestResults(Optional<PackageBuild> releaseBuild) {
    Map<String, PackageTestResult> testMap = new HashMap<>();
    if(releaseBuild.isPresent()) {
      QueryResultIterable<PackageTestResult> testResults = PackageDatabase.getTestResults(releaseBuild.get().getId());
      for (PackageTestResult testResult : testResults) {
        testMap.put(testResult.getName(), testResult);
      }
    }
    return testMap;
  }

  private Optional<PackageBuild> findReleaseBuild(PackageVersionId pvid) {
    Optional<PackageVersion> packageVersion = PackageDatabase.getPackageVersion(pvid);
    if(!packageVersion.isPresent()) {
      throw new WebApplicationException(Response
          .status(Response.Status.BAD_REQUEST)
          .entity("No such package version")
          .build());
    }
    PackageBuildId releaseBuildId = packageVersion.get().getLastBuildId();
    if(releaseBuildId == null) {
      return Optional.absent();
    }
    return Optional.of(PackageDatabase.getBuild(releaseBuildId).safe());
  }
}
