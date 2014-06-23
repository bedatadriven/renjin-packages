package org.renjin.build.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.hibernate.LockMode;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.renjin.build.HibernateUtil;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.*;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
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
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/queue/launch/all"));

    return Response.temporaryRedirect(uri.getRequestUriBuilder().replacePath("/queue/dashboard").build()).build();
  }

  @POST
  @Path("all")
  private void launch(String renjinCommitId) {


    final StatelessSession session = HibernateUtil.openStatelessSession();
    Transaction transaction = session.beginTransaction();

    try {


      RenjinCommit commit = (RenjinCommit)session.get(RenjinCommit.class, renjinCommitId);

      Build build = new Build();
      build.setRenjinCommit(commit);
      build.setStarted(new Date());
      session.insert(build);

      List<Object[]> deps = session.createSQLQuery(
          "SELECT p.id, d.dependency_id FROM RPackageVersion p " +
              "LEFT JOIN RPackageDependency d ON (p.id = d.version_id) order by p.id")
          .list();

      String packageId = null;
      List<String> dependencies = Lists.newArrayList();

      int count = 0;

      for(Object[] pair : deps) {
        if(!Objects.equals(packageId, pair[0])) {
          if(count > 100) {
            break;
          }
          scheduleBuild(session, build, packageId, dependencies);
          count ++;
          dependencies.clear();
          packageId = (String)pair[0];
        }
        String dependencyVersionId = (String)pair[1];

        if(dependencyVersionId != null) {
          // for now we expect to build within the same batch
          dependencyVersionId += "-b" + build.getId();
          dependencies.add(dependencyVersionId);
        }
      }

      scheduleBuild(session, build, packageId, dependencies);

      transaction.commit();

      System.out.println("started build " + build.getId());

    } catch(Exception e) {
      transaction.rollback();
      throw new RuntimeException(e);
    }

    BuildQueueController.schedule();
  }

  private void scheduleBuild(StatelessSession session, Build build, String packageId, List<String> dependencies) {
    if(packageId != null) {
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
        packageBuild.setStage(BuildStage.WAITING);
      }
      session.insert(packageBuild);
    }
  }

  public static void main(String[] args) {
    new BuildLauncher().launch("cbf6435939168a0a527bdf97580bdb1f3f2ea264");
  }
}
