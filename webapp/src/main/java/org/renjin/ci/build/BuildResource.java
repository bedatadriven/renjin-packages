package org.renjin.ci.build;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.*;
import org.renjin.ci.storage.StorageKeys;
import org.renjin.ci.task.PackageBuildResult;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.nio.channels.Channels.newInputStream;

public class BuildResource {

  private static final Logger LOGGER = Logger.getLogger(BuildResource.class.getName());

  private PackageVersionId packageVersionId;
  private long buildNumber;


  public BuildResource(PackageVersionId packageVersionId, long buildNumber) {
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
        
        // Retrieve the current status of this package version and the build itself
        PackageBuild build;
        PackageStatus status;
        try {
          build = ofy().load().key(PackageBuild.key(packageVersionId, buildNumber)).safe();
          status = ofy().load().key(PackageStatus.key(packageVersionId, build.getRenjinVersionId())).safe();
        } catch (NotFoundException notFoundException) {
          LOGGER.info("No PackageStatus found for " + packageVersionId + "-b" + buildNumber + ".");
          return;
        }

        // Has the status already been reported?
        if(build.getOutcome() != null) {
          LOGGER.log(Level.INFO, "Build " + build.getId() + " is already marked as " + build.getOutcome());
          if(build.getStartTime() != null) {
            LOGGER.log(Level.INFO, "Setting startTime to NULL");
            build.setStartTime(null);
            ofy().save().entities(build).now();
          }
          return;
        }

        // Has someone else gotten here first?
        if(!status.getBuildStatus().equals(BuildStatus.BUILDING) ||
           !Objects.equals(status.getBuildNumber(), build.getBuildNumber())) {

          LOGGER.log(Level.INFO, "Build " + build.getId() + " has been superseded by " + status.getBuildNumber());
          return;
        }

        LOGGER.log(Level.INFO, "Marking " + build.getId() + " as " + buildResult.getOutcome());

        build.setOutcome(buildResult.getOutcome());
        build.setEndTime(System.currentTimeMillis());
        build.setDuration(build.getEndTime() - build.getStartTime());
        build.setNativeOutcome(buildResult.getNativeOutcome());
        build.setStartTime(null);

        if(build.getOutcome() == BuildOutcome.SUCCESS) {
          status.setBuildStatus(BuildStatus.BUILT);
        } else {
          status.setBuildStatus(BuildStatus.FAILED);
        }

        ofy().save().entities(build, status);
      }
    });
  }
}
