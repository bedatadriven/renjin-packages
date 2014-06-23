package org.renjin.build.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

/**
 * Supervises the build queue:
 * <ul>
 *   <li>Marks dependencies as resolved</li>
 *   <li>Frees leases on timed out workers</li>
 *   <li>If there are no </li>
 * </ul>
 */
public class BuildQueueController  {

  @POST
  public Response execute() {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    em.getTransaction().begin();

    freeExpiredLeases(em);
    int scheduledBuilds = countScheduledBuilds();

  //    resolveDependencies(em);

    em.getTransaction().commit();

    if(scheduledBuilds > 0) {
      schedule();
    }

    return Response.ok().build();
  }

  private void resolveDependencies(EntityManager em, int buildId) {


    List<RPackageBuild> builds = em.createQuery("SELECT b FROM RPackageBuild b " +
        "where b.stage = 'WAITING'", RPackageBuild.class)
        .setMaxResults(100)
        .getResultList();

    for(RPackageBuild build : builds) {
      if(isDepResolved(em, build)) {
        build.setStage(BuildStage.READY);
      }
    }
  }

  private boolean isDepResolved(EntityManager em, RPackageBuild build) {
    for(RPackageDependency dep : build.getPackageVersion().getDependencies()) {
      List<RPackageBuild> depBuilds = em.createQuery(
          "select b from RPackageBuild b where b.packageVersion = :dep and b.outcome='SUCCESS'", RPackageBuild.class)
          .getResultList();
      if(depBuilds.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private void freeExpiredLeases(EntityManager em) {
    long cutOff = new DateTime().minusHours(1).getMillis() / 1000;

    em.createNativeQuery(
        "UPDATE RPackageBuild set stage = 'READY', leased = NULL, leaseTime = NULL " +
        "WHERE stage = 'LEASED' and unix_timestamp(leaseTime) < ?1")
            .setParameter(1, cutOff)
            .executeUpdate();
  }

  private int countScheduledBuilds() {
    return ((Number)HibernateUtil
        .getActiveEntityManager()
        .createNativeQuery("SELECT COUNT(*) FROM RPackageBuild WHERE stage != 'TERMINATED'")
        .getSingleResult()).intValue();
  }

  public static void schedule() {
    if(SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
      QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/queue/control").countdownMillis(60_000));
    }
  }

  public static void main(String[] args) {
    new BuildQueueController().execute();
  }
}
