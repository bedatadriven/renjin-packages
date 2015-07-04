package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;
import org.renjin.ci.stats.StatTasks;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

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

  @POST
  @Consumes("application/json")
  public void postResult(final PackageBuildResult buildResult) {

    LOGGER.info("Received build results for " + packageVersionId + "-b" + buildNumber + ": " + buildResult.getOutcome());

    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        Key<PackageBuild> buildKey = PackageBuild.key(packageVersionId, buildNumber);

        // Retrieve the current status of this package version and the build itself
        PackageBuild build;
        try {
          build = ofy().load().key(buildKey).safe();
        } catch (NotFoundException notFoundException) {
          LOGGER.info("Cannot find PackageBuild entity to update: " + buildKey);
          return;
        }

        // Has the status already been reported?
        if(build.getOutcome() != null) {
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
          if (updateDeltaFlags(packageVersionId, Optional.<PackageBuild>absent())) {
            StatTasks.scheduleBuildDeltaCountUpdate();
          }

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

  /**
   * Examine the results of builds of this package version against sequential versions of Renjin and identify
   * builds that represent "regressions" and "progressions".
   *
   * A regression is evidence that a change in Renjin has led to a failure in package building. A progression
   * is evidence that a change in Renjin has fixed an existing defect in the Renjin build process.
   *
   * For example, if survey:2.5 fails to build against
   * Renjin 0.7.1514, but successfully builds against Renjin 0.7.1534, then this is an improvement
   * we want to highlight. 
   *
   * <p>However, if we try to build again with Renjin 0.7.1560 and it fails, then that PackageBuild is
   * a regression to flag immediately.</p>
   *
   * @return true, if any flags have been changed
   */
  public static boolean updateDeltaFlags(PackageVersionId packageVersionId, Optional<PackageBuild> newBuild) {


    List<PackageBuild> builds = PackageDatabase.getFinishedBuilds(packageVersionId);
    if(newBuild.isPresent()) {
      builds.add(newBuild.get());
    }

    if (builds.isEmpty()) {
      return false;
    }

    Set<Object> toSave = new HashSet<>();


    // Build a simplified list, mapping each renjin version in order to a build
    // If there have been multiple builds for a given Renjin Version, use the last build
    TreeMap<RenjinVersionId, PackageBuild> buildMap = Maps.newTreeMap();
    for (PackageBuild build : builds) {
      RenjinVersionId rv = build.getRenjinVersionId();
      if(isValidRenjinVersion(rv)) {
        PackageBuild lastBuild = buildMap.get(rv);

        if (lastBuild == null || build.getBuildNumber() > lastBuild.getBuildNumber()) {
          buildMap.put(rv, build);
        }
      }
    }

    // Walk the Renjin versions and tag improvements
    PackageBuild lastBuild = null;

    for (PackageBuild build : buildMap.values()) {
      byte newDelta;
      if (lastBuild == null || lastBuild.isSucceeded() == build.isSucceeded()) {
        newDelta = 0; // no change            
      } else if (lastBuild.isSucceeded() && build.isFailed()) {
        newDelta = -1; // regression
      } else {
        newDelta = +1;
      }

      if (build.getBuildDelta() != newDelta) {
        build.setBuildDelta(newDelta);
        toSave.add(build);
      }

      LOGGER.info(String.format("%s @ Renjin %s: Build %d (%+d)",
          packageVersionId,
          build.getRenjinVersion(),
          build.getBuildNumber(),
          build.getBuildDelta()));

      lastBuild = build;
    }

    // Walk the sequence of versions again and tag regressions/improvements in compilation

    lastBuild = null;

    for (PackageBuild build : buildMap.values()) {
      if(build.getNativeOutcome() != null && build.getNativeOutcome() != NativeOutcome.NA) {
        byte newDelta;

        if(lastBuild == null || lastBuild.getNativeOutcome() == build.getNativeOutcome()) {
          newDelta = 0;
        } else if(lastBuild.getNativeOutcome() == NativeOutcome.SUCCESS && build.getNativeOutcome() == NativeOutcome.FAILURE) {
          newDelta = -1;
        } else {
          newDelta = 1;
        }

        if(build.getCompilationDelta() != newDelta) {
          build.setCompilationDelta(newDelta);
          toSave.add(build);
        }


        LOGGER.info(String.format("%s @ Renjin %s: Compilation %d (%+d)",
            packageVersionId,
            build.getRenjinVersion(),
            build.getBuildNumber(),
            build.getCompilationDelta()));

        lastBuild = build;
      }
    }

    // Clear the deltas of any builds that have been superceded and ignored here
    for (PackageBuild build : builds) {
      if(!buildMap.containsValue(build)) {
        if(build.getBuildDelta() != 0) {
          build.setBuildDelta((byte)0);
          toSave.add(build);
        }
        if(build.getCompilationDelta() != 0) {
          build.setCompilationDelta((byte)0);
          toSave.add(build);
        }
      }
    }

    ObjectifyService.ofy().save().entities(toSave);

    return true;
  }

  private static boolean isValidRenjinVersion(RenjinVersionId rv) {
    if(rv.toString().contains("SNAPSHOT")) {
      return false;
    }
    if(rv.toString().equals("LATEST")) {
      return false;
    }
    return true;
  }
}
