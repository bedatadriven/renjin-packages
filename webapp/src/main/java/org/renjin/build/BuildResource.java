package org.renjin.build;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.build.archive.GcsAppIdentityServiceUrlSigner;
import org.renjin.build.model.*;
import org.renjin.build.model.Package;
import org.renjin.build.task.PackageBuildResult;
import org.renjin.build.task.PackageBuildTask;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/build")
public class BuildResource {

  private static final Logger LOGGER = Logger.getLogger(BuildResource.class.getName());

  GcsAppIdentityServiceUrlSigner signer = new GcsAppIdentityServiceUrlSigner();

  @GET
  @Produces("text/html")
  public Viewable getIndex() {

    Map<String, Object> model = Maps.newHashMap();
    model.put("building", ofy().load().type(PackageBuild.class).order("-startTime").limit(10).list());
    model.put("recent", ofy().load().type(PackageBuild.class).order("-endTime").limit(30).list());

    return new Viewable("/buildQueue.ftl", model);

  }

  @GET
  @Produces("text/html")
  @Path("{groupId}/{packageName}/{version}/{buildNumber}")
  public Viewable get(
      @PathParam("groupId") String groupId,
      @PathParam("packageName") String packageName,
      @PathParam("version") String version,
      @PathParam("buildNumber") long buildNumber) {

    PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, version);
    PackageBuild build = PackageDatabase.getBuild(packageVersionId, buildNumber);
    PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

    List<PackageBuild> previousBuilds = PackageDatabase.getBuilds(packageVersionId);

    Map<String, Object> model = Maps.newHashMap();
    model.put("groupId", groupId);
    model.put("packageName", packageName);
    model.put("version", version);
    model.put("buildNumber", buildNumber);
    model.put("build", build);
    model.put("package", packageVersion);
    model.put("description", packageVersion.parseDescription());
    model.put("startTime", new Date(build.getStartTime()));
    model.put("previousBuilds", previousBuilds);

    return new Viewable("/buildResult.ftl", model);
  }

  @POST
  @Path("next")
  @Produces("application/json")
  public Response getNextBuild(@FormParam("worker") String workerId) throws Exception {

    Optional<PackageStatus> next = PackageDatabase.getNextReady();
    if(!next.isPresent()) {
      return Response.status(Response.Status.NO_CONTENT).build();
    }

    PackageBuildTask task = createNewBuild(workerId, next.get());
    return Response.ok(task).build();
  }

  private PackageBuildTask createNewBuild(final String workerId, final PackageStatus status) throws Exception {

    PackageBuild build = ofy().transact(new Work<PackageBuild>() {
      @Override
      public PackageBuild run() {
        PackageVersion packageVersion = ofy().load().key(status.getPackageVersionId().key()).safe();
        packageVersion.setLastBuildNumber(packageVersion.getLastBuildNumber() + 1);

        PackageBuild build = new PackageBuild(status.getPackageVersionId(), packageVersion.getLastBuildNumber());
        build.setRenjinVersion(status.getRenjinVersionId().toString());
        build.setDependencies(status.getDependencies());
        build.setStartTime(System.currentTimeMillis());
        build.setWorkerId(workerId);

        PomBuilder pomBuilder = new PomBuilder(build, packageVersion.parseDescription());
        build.setPom(pomBuilder.getXml());

        status.setBuildStatus(BuildStatus.BUILDING);
        status.setBuildNumber(build.getBuildNumber());

        ofy().save().entities(packageVersion, build, status);

        return build;
      }
    });

    PackageBuildTask task = new PackageBuildTask();
    task.setPackageVersionId(build.getPackageVersionId().toString());
    task.setBuildNumber(build.getBuildNumber());
    task.setPom(build.getPom());
    return task;
  }

  @POST
  @Consumes("application/json")
  @Path("{groupId}/{packageName}/{version}/{buildNumber}")
  public void postResult(
      @PathParam("groupId") String groupId,
      @PathParam("packageName") String packageName,
      @PathParam("version") String version,
      @PathParam("buildNumber") final long buildNumber,
      final PackageBuildResult buildResult) {

    PackageDatabase packageDatabase = new PackageDatabase();

    final PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, version);

    LOGGER.info("Received build results for " + packageVersionId + "-b" + buildNumber + ": " + buildResult.getOutcome());

    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        PackageBuild build = ofy().load().key(PackageBuild.key(packageVersionId, buildNumber)).safe();
        PackageStatus status = ofy().load().key(PackageStatus.key(packageVersionId, build.getRenjinVersionId())).safe();

        // make sure someone else didn't get here first
        if(build.getOutcome() == null &&
           status.getBuildStatus().equals(BuildStatus.BUILDING) &&
           status.getBuildNumber() == buildNumber) {

          build.setOutcome(buildResult.getOutcome());
          build.setEndTime(System.currentTimeMillis());

          if(build.getOutcome() == BuildOutcome.SUCCESS) {
            status.setBuildStatus(BuildStatus.BUILT);
          } else {
            status.setBuildStatus(BuildStatus.FAILED);
          }

          QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/tasks/updateStatus")
            .param("packageVersionId", packageVersionId.toString())
            .param("buildNumber", Long.toString(buildNumber)));

          ofy().save().entities(build, status);
        }
      }
    });
  }
}
