package org.renjin.ci.build;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Maps;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.*;
import org.renjin.ci.task.PackageBuildResult;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

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
  public Viewable get() {
    PackageBuild build = PackageDatabase.getBuild(packageVersionId, buildNumber);
    PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

    List<PackageBuild> previousBuilds = PackageDatabase.getBuilds(packageVersionId).list();

    Map<String, Object> model = Maps.newHashMap();
    model.put("groupId", packageVersion.getGroupId());
    model.put("packageName", packageVersion.getPackageVersionId().getPackageName());
    model.put("version", packageVersion.getPackageVersionId().getVersion());
    model.put("buildNumber", buildNumber);
    model.put("build", build);
    model.put("package", packageVersion);
    model.put("description", packageVersion.parseDescription());
    model.put("startTime", build.getStartDate());
    model.put("previousBuilds", previousBuilds);

    return new Viewable("/buildResult.ftl", model);
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
