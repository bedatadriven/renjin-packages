package org.renjin.ci.build;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.QueryKeys;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.*;
import org.renjin.ci.pipelines.Pipelines;
import org.renjin.ci.task.PackageBuildTask;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/build/queue")
public class BuildQueue {

  private static final Logger LOGGER = Logger.getLogger(BuildQueue.class.getName());

  @GET
  @Produces("text/html")
  public Viewable getIndex() {

    Map<String, Object> model = Maps.newHashMap();
    model.put("building", ofy().load()
        .type(PackageBuild.class)
        .order("-startTime").limit(30).list());
    model.put("recent", ofy().load().type(PackageBuild.class).order("-endTime").limit(30).list());

    return new Viewable("/buildQueue.ftl", model);
  }

  /**
   * Leases the next PackageStatus with a READY buildStatus to the calling worker.
   * @param workerId the instance id of the worker requesting the job
   */
  @POST
  @Path("next")
  @Produces("application/json")
  public Response getNextBuild(@FormParam("worker") String workerId) throws Exception {

    LOGGER.log(Level.INFO, "Worker " + workerId + " request lease on package build");

    Optional<Key<PackageStatus>> next = PackageDatabase.getNextReady();
    if(!next.isPresent()) {
      LOGGER.log(Level.INFO, "No builds are READY.");

    } else {
      try {
        LOGGER.log(Level.INFO, "Trying to lease " + next.get().getName());

        PackageBuildTask task = createNewBuild(workerId, next.get());

        LOGGER.log(Level.INFO, "Leased new build " + task);

        return Response.ok(task).build();
      } catch(Exception e) {
        LOGGER.log(Level.WARNING, "Exception creating new build", e);
      }
    }

    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /**
   * Creates a new PackageBuild record initially leased to the calling worker, and
   * updates the PackageStatus record to 'BUILDING'
   */
  private PackageBuildTask createNewBuild(final String workerId, final Key<PackageStatus> statusKey) throws Exception {

    PackageBuild build = ofy().transact(new Work<PackageBuild>() {
      @Override
      public PackageBuild run() {

        PackageStatus status = ofy().load().key(statusKey).safe();
        if(status.getBuildStatus() != BuildStatus.READY) {
          throw new IllegalStateException("Build has already been leased");
        }

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


  /**
   * Following the completion of a successful package build, update the PackageStatus for
   * any other package that was blocked by this package.
   *
   * @param newlyBuiltPackageVersionId the packageVersionId of the package that completed successfully
   * @param buildNumber the build number of the successful build
   */
  @POST
  @Path("queueDownstream")
  public void queueDownstream(
      @FormParam("packageVersionId") final String newlyBuiltPackageVersionId,
      @FormParam("buildNumber") final long buildNumber) {

    LOGGER.log(Level.INFO, "Update downstream package status records of " + newlyBuiltPackageVersionId);

    QueryKeys<PackageStatus> downstream = ofy().load().type(PackageStatus.class)
        .filter("blockingDependencies = ", newlyBuiltPackageVersionId)
        .keys();

    for(final Key<PackageStatus> key : downstream) {

      LOGGER.log(Level.INFO, "Downstream: " + key.getName());

      ofy().transact(new VoidWork() {
        @Override
        public void vrun() {
          PackageStatus status = ofy().load().key(key).now();

          LOGGER.log(Level.INFO, status.getId() + " is downstream.");
          LOGGER.log(Level.INFO, "dependencies = " + status.getDependencies() +
              " blockingDeps = " + status.getBlockingDependencies());

          if (status.getBlockingDependencies().remove(newlyBuiltPackageVersionId)) {
            status.getDependencies().add(newlyBuiltPackageVersionId + "-b" + buildNumber);

            if (status.getBuildStatus() == BuildStatus.BLOCKED &&
                status.getBlockingDependencies().isEmpty()) {

              status.setBuildStatus(BuildStatus.READY);
            }

            LOGGER.log(Level.INFO, "After update:: dependencies = " + status.getDependencies() +
                " blockingDeps = " + status.getBlockingDependencies());

            ofy().save().entity(status);
          }
        }
      });
    }
  }

  @POST
  @Path("resetStatus")
  public Response resetStatus() {
    Pipelines.applyAll(new TimeoutAllBuilds());
    return Pipelines.redirectToStatus(Pipelines.applyAll(new ResetPackageStatus()));
  }

  @POST
  @Path("retryBuilds")
  public Response retryBuild(@FormParam("renjinVersion") String renjinVersion) {
    // TODO
//    QueryResultIterable<PackageBuild> builds = ofy().load()
//        .type(PackageStatus.class).filter("buildStatus", "BUILT").iterable();
//
//    for(PackageStatus status : built) {
//      LOGGER.log(Level.INFO, status.getId() + ": renjinVersion = " + status.getRenjinVersionId());
//      if(status.getRenjinVersionId().equals(RenjinVersionId.RELEASE)) {
//        QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/build/queue/queueDownstream")
//            .param("packageVersionId", status.getPackageVersionId().toString())
//            .param("buildNumber", Long.toString(status.getBuildNumber())));
//      }
//    }
    return Response.ok().build();
  }


  @POST
  @Path("retry")
  public Response retryBuilds(@FormParam("renjinVersion") String renjinVersion) throws URISyntaxException {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/build/queue/retryBuilds")
        .param("renjinVersion", renjinVersion));

    return Response.seeOther(new URI("/build/queue")).build();
  }

  @GET
  @Path("cleanup")
  public Response cleanupQueue() {
    // builds that started before this time and haven't finished will
    // be killed
    long cutOff = System.currentTimeMillis() - (20 * 60 * 1000); // 20 min cut off

    QueryResultIterable<Key<PackageBuild>> building = ofy().load()
        .type(PackageBuild.class)
        .filter("startTime <", cutOff)
        .keys()
        .iterable();

    for(Key<PackageBuild> buildKey : building) {
      LOGGER.log(Level.INFO, "Killing " + buildKey.getName());

      String[] parts = buildKey.getName().split(":");
      QueueFactory.getDefaultQueue().add(TaskOptions.Builder
          .withTaskName(buildKey.getName())
          .url("/build/result/" + parts[0] + "/" + parts[1] + "/" + parts[2] + "/" + parts[3] + "/timeout"));
    }

    return Response.ok().build();
  }

}