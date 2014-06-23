package org.renjin.build.queue;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.util.Arrays;
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

    resolveDependencies(em);

    em.getTransaction().commit();

    if(scheduledBuilds > 0) {
      schedule();
    }

    return Response.ok().build();
  }

  private void resolveDependencies(EntityManager em) {

    List<Tuple> waiting = em.createQuery(
        "SELECT b.id, b.dependencyVersions from RPackageBuild b WHERE b.stage = 'WAITING'", Tuple.class)
        .getResultList();

    Set<String> blockingBuildIds = Sets.newHashSet();
    for(Tuple tuple : waiting) {
      String[] depIds = Strings.nullToEmpty(tuple.get(1, String.class)).split(",");
      blockingBuildIds.addAll(Arrays.asList(depIds));
    }

    Set<String> successfulBuildIds = Sets.newHashSet(em.createQuery("SELECT b.id from RPackageBuild b WHERE b.outcome = 'SUCCESS' and " +
        "b.id IN (:ids)", String.class)
        .setParameter("ids", blockingBuildIds)
        .getResultList());


    Set<String> readyBuildIds = Sets.newHashSet();
    for(Tuple tuple : waiting) {
      String[] depIds = Strings.nullToEmpty(tuple.get(1, String.class)).split(",");
      if(containsAll(depIds, successfulBuildIds)) {
        readyBuildIds.add(tuple.get(0, String.class));
      }
    }

    if(!readyBuildIds.isEmpty()) {
      em.createQuery("UPDATE RPackageBuild b SET b.stage = 'READY' WHERE b.id in (:ids)")
          .setParameter("ids", readyBuildIds)
          .executeUpdate();
    }
  }

  private boolean containsAll(String[] depIds, Set<String> successfullBuildIds) {
    for(int i =0;i!=depIds.length;++i) {
      if(!successfullBuildIds.contains(depIds[i])) {
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
