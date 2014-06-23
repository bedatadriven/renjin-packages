package org.renjin.build.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;


/**
 * Schedules a set of package build tasks
 */
public class BuildLauncher {


  @POST
  public Response startBuild(
                           @Context UriInfo uri,
                           @FormParam("renjinCommitId") String renjinCommitId) {

    // It takes quite awhile to schedule all 10k packages, so queue this as a longer-running task
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder
        .withUrl("/queue/launch/all")
        .param("renjinCommitId", renjinCommitId));

    return Response.seeOther(uri.getRequestUriBuilder().replacePath("/queue/dashboard").build()).build();
  }

  @POST
  @Path("all")
  public void launch(@FormParam("renjinCommitId") String renjinCommitId) {


    final StatelessSession session = HibernateUtil.openStatelessSession();
    Transaction transaction = session.beginTransaction();

    try {


      RenjinCommit commit = (RenjinCommit)session.get(RenjinCommit.class, renjinCommitId);

      Build build = new Build();
      build.setRenjinCommit(commit);
      build.setStarted(new Date());
      session.insert(build);

      List<Object[]> successfulBuilds = session.createSQLQuery(
          "SELECT pb.packageVersion_id, pb.id FROM RPackageBuild pb LEFT JOIN Build b ON (pb.build_id = b.id)" +
              " WHERE pb.outcome = 'SUCCESS' and build_id > 150 order by build_id")
          .list();

      Map<String, String> successfulBuildIds = Maps.newHashMap();
      for(Object[] successfulBuild : successfulBuilds) {
        String versionId = (String) successfulBuild[0];
        String buildId = (String) successfulBuild[1];
        successfulBuildIds.put(versionId, buildId);
      }

      List<Object[]> deps = session.createSQLQuery(
          "SELECT p.id, d.dependency_id FROM RPackageVersion p " +
              "LEFT JOIN RPackageDependency d ON (p.id = d.version_id) " +
              "WHERE p.id  NOT IN (select packageVersion_id FROM RPackageBuild b " +
                                 "   WHERE b.stage != 'COMPLETE') " +
              "ORDER by p.id")
          .list();

      String packageId = null;
      List<String> dependencies = Lists.newArrayList();

      int count = 0;

      for(Object[] pair : deps) {
        if(!Objects.equals(packageId, pair[0])) {
          scheduleBuild(session, build, packageId, successfulBuildIds, dependencies);
          count ++;
          dependencies.clear();
          packageId = (String)pair[0];
        }
        String dependencyVersionId = (String)pair[1];

        if(dependencyVersionId != null) {
          if(successfulBuildIds.containsKey(dependencyVersionId)) {
            dependencies.add(successfulBuildIds.get(dependencyVersionId));
          } else {
            // should be part of this batch
            dependencyVersionId += "-b" + build.getId();
            dependencies.add(dependencyVersionId);
          }
        }
      }

      scheduleBuild(session, build, packageId, successfulBuildIds, dependencies);

      transaction.commit();

      System.out.println("started build " + build.getId());

    } catch(Exception e) {
      transaction.rollback();
      throw new RuntimeException(e);
    }

    BuildQueueController.schedule();
  }

  private void scheduleBuild(StatelessSession session, Build build, String packageId,
                             Map<String, String> successfulBuildIds, List<String> dependencies) {
    if(packageId != null && !successfulBuildIds.containsKey(packageId)) {
      RPackageVersion version = new RPackageVersion();
      version.setId(packageId);

      RPackageBuild packageBuild = new RPackageBuild();
      packageBuild.setId(packageId + "-b" + build.getId());
      packageBuild.setBuild(build);
      packageBuild.setPackageVersion(version);

      if(dependencies.isEmpty()) {
        packageBuild.setStage(BuildStage.READY);
      } else {
        packageBuild.setDependencyVersions(Joiner.on(",").join(dependencies));
        if(successfulBuildIds.values().containsAll(dependencies)) {
          packageBuild.setStage(BuildStage.READY);
        } else {
          packageBuild.setStage(BuildStage.WAITING);
        }
      }
      session.insert(packageBuild);
    }
  }

  public static void main(String[] args) {
    new BuildLauncher().launch("cbf6435939168a0a527bdf97580bdb1f3f2ea264");
  }
}
