package org.renjin.ci.packages;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import com.googlecode.objectify.*;
import com.googlecode.objectify.NotFoundException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;
import org.renjin.ci.stats.StatTasks;
import org.renjin.ci.storage.StorageKeys;
import org.renjin.ci.task.PackageBuildResult;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.nio.channels.Channels.newInputStream;

public class PackageBuildResource {

  private static final Logger LOGGER = Logger.getLogger(PackageBuildResource.class.getName());

  private PackageVersionId packageVersionId;
  private long buildNumber;


  public PackageBuildResource(PackageVersionId packageVersionId, long buildNumber) {
    this.packageVersionId = packageVersionId;
    this.buildNumber = buildNumber;
  }

  @GET
  @Produces("text/html")
  public Viewable get() throws IOException {

    // Start fetching list of builds
    Iterable<PackageBuild> builds = PackageDatabase.getBuilds(packageVersionId).iterable();

    // Start fetching log text
    String logText = tryFetchLog();

    Map<String, Object> model = Maps.newHashMap();
    model.put("groupId", packageVersionId.getGroupId());
    model.put("packageName", packageVersionId.getPackageName());
    model.put("version", packageVersionId.getVersionString());
    model.put("buildNumber", buildNumber);
    model.put("builds", Lists.newArrayList(builds));
    model.put("build", findBuild(builds));
    model.put("log", logText);

    return new Viewable("/buildResult.ftl", model);
  }

  private PackageBuild findBuild(Iterable<PackageBuild> builds) {
    for (PackageBuild build : builds) {
      if(build.getBuildNumber() == buildNumber) {
        return build;
      }
    }
    throw new WebApplicationException(Response.Status.NOT_FOUND);
  }

  private String tryFetchLog() {

    String logUrl = "http://storage.googleapis.com/renjinci-logs/" + StorageKeys.buildLog(packageVersionId, buildNumber);
    try {
      byte[] bytes = Resources.toByteArray(new URL(logUrl));
      if(bytes.length >= 2 && bytes[0] == (byte)0x1f && bytes[1] == (byte)0x8b) {
        try(Reader reader = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), Charsets.UTF_8)) {
          return CharStreams.toString(reader);
        }
      } else {
        return new String(bytes, Charsets.UTF_8);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error reading " + logUrl, e);
      return null;
    }
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

          LOGGER.log(Level.INFO, "Marking " + build.getId() + " as " + buildResult.getOutcome());

          build.setOutcome(buildResult.getOutcome());
          build.setEndTime(System.currentTimeMillis());
          build.setDuration(build.getEndTime() - build.getStartTime());
          build.setNativeOutcome(buildResult.getNativeOutcome());
          ObjectifyService.ofy().save().entity(build);

          maybeUpdateLastSuccessfulBuild(build);

          // Update the delta (regression/progression) flags for this build
          if(updateDeltaFlags(packageVersionId, Optional.<PackageBuild>absent())) {
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

    List<Object> toSave = new ArrayList<>();


    // Build a simplified list, mapping each renjin version in order to a build
    // If there have been multiple builds for a given Renjin Version, use the last build
    TreeMap<RenjinVersionId, PackageBuild> buildMap = Maps.newTreeMap();
    for (PackageBuild build : builds) {
      RenjinVersionId rv = build.getRenjinVersionId();
      PackageBuild lastBuild = buildMap.get(rv);

      if (lastBuild == null || build.getBuildNumber() > lastBuild.getBuildNumber()) {
        buildMap.put(rv, build);
      }
    }

    // Walk the Renjin versions and tag regressions/improvements


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

      LOGGER.info(String.format("Renjin %s: Build %d (%+d)",
          build.getRenjinVersion(),
          build.getBuildNumber(),
          build.getBuildDelta()));

      lastBuild = build;
    }

    // Clear the deltas of any builds that have been superceded and ignored here
    for (PackageBuild build : builds) {
      if(!buildMap.containsValue(build)) {
        if(build.getBuildDelta() != 0) {
          build.setBuildDelta((byte)0);
          toSave.add(build);
        }
      }
    }

    ObjectifyService.ofy().save().entities(toSave);

    return true;
  }
}
