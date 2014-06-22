package org.renjin.build.queue;

import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.*;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import java.util.Date;
import java.util.List;


/**
 * Schedules a set of package build tasks
 */
public class BuildLauncher {

  private final EntityManager entityManager = PersistenceUtil.createEntityManager();

  @POST
  public String startBuild(@FormParam("renjinVersion") String renjinVersion,
                           @FormParam("limit") Integer limit) {

    RenjinCommit commit = entityManager.createQuery(
        "select c from RenjinCommit c where c.version = :v", RenjinCommit.class)
        .setParameter("v", renjinVersion)
        .getSingleResult();

    int launchCount = 0;

    entityManager.getTransaction().begin();

    try {
      Build build = new Build();
      build.setRenjinCommit(commit);
      build.setStarted(new Date());
      entityManager.persist(build);

      List<Tuple> versions = entityManager.createQuery(
          "select v.id, count(d.id) from RPackageVersion v " +
              "LEFT JOIN v.dependencies d GROUP BY v.id order by count(v.id) asc", Tuple.class)
          .setMaxResults(20)
          .getResultList();

      for(Tuple version : versions) {

        String versionId = (String) version.get(0);
        long numDeps = version.get(1, Long.class);

        RPackageBuild packageBuild = new RPackageBuild();
        packageBuild.setBuild(build);
        packageBuild.setPackageVersion(entityManager.getReference(RPackageVersion.class, versionId));
        if(numDeps == 0) {
          packageBuild.setStage(BuildStage.READY);
        } else {
          packageBuild.setStage(BuildStage.WAITING);
        }
        packageBuild.setDependenciesResolved(numDeps == 0);
        entityManager.persist(packageBuild);

        launchCount++;

        if(limit!=null && launchCount > limit) {
          break;
        }
      }
      entityManager.getTransaction().commit();

    } catch(Exception e) {
      entityManager.getTransaction().rollback();
      throw new RuntimeException(e);
    }
    entityManager.close();

    BuildQueueController.schedule();

    return "Queued " + launchCount + " builds";
  }

  public static void main(String[] args) {
    new BuildLauncher().startBuild("0.7.0-RC7", 10);
  }
}
