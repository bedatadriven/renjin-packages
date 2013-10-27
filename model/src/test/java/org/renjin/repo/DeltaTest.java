package org.renjin.repo;

import org.junit.Test;
import org.renjin.repo.model.RPackageBuildResult;

import javax.persistence.EntityManager;
import java.util.List;

public class DeltaTest {

  @Test
  public void test() {

    DeltaCalculator preds = new DeltaCalculator(PersistenceUtil.createEntityManager(),
      "412d8b64b20eef7cdab7fec380d0bc1bbb679edc");
    List<Integer> parentBuilds = preds.getPredecessors();
    System.out.println(parentBuilds);

    int buildId = 55;

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();

    // first set all the build results to null
//    em.createQuery("UPDATE RPackageBuildResult r SET r.delta = NULL where r.build.id = :buildId")
//      .setParameter("buildId", buildId)
//      .executeUpdate();

    // get the list of regressions
//    List<RPackageBuildResult> regressions = queryChangeType(parentBuilds, buildId, em);
//
//
//    for(RPackageBuildResult regression : regressions) {
//      System.out.println("regression: " + regression.getPackageVersion().getId());
//    }
//
//    List<RPackageBuildResult> improvements = em.createQuery("SELECT r FROM RPackageBuildResult r WHERE r.build.id = :buildId and " +
//      "r.outcome = org.renjin.repo.model.BuildOutcome.SUCCESS and " +
//      "r.packageVersion in (SELECT pr.packageVersion FROM RPackageBuildResult pr WHERE pr.build.id = :parentBuildId and " +
//      "pr.outcome != org.renjin.repo.model.BuildOutcome.SUCCESS)", RPackageBuildResult.class)
//      .setParameter("buildId", buildId)
//      .setParameter("parentBuildId", parentBuilds.get(0))
//      .getResultList();
//
//
//    for(RPackageBuildResult regression : improvements) {
//      System.out.println("improvement: " + regression.getPackageVersion().getId());
//    }
//
//    List<RPackageBuildResult> stillSucceeding = em.createQuery("SELECT r FROM RPackageBuildResult r WHERE r.build.id = :buildId and " +
//      "r.outcome = org.renjin.repo.model.BuildOutcome.SUCCESS and " +
//      "r.packageVersion in (SELECT pr.packageVersion FROM RPackageBuildResult pr WHERE pr.build.id = :parentBuildId and " +
//      "pr.outcome = org.renjin.repo.model.BuildOutcome.SUCCESS)", RPackageBuildResult.class)
//      .setParameter("buildId", buildId)
//      .setParameter("parentBuildId", parentBuilds.get(0))
//      .getResultList();
//
//
//
//    em.getTransaction().commit();


  }
}
