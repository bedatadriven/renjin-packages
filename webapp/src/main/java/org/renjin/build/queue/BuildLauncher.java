package org.renjin.build.queue;

import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.Build;
import org.renjin.build.model.RPackageBuild;
import org.renjin.build.model.RPackageVersion;
import org.renjin.build.model.RenjinCommit;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.util.Date;
import java.util.List;


public class BuildLauncher {

  private final EntityManager entityManager = PersistenceUtil.createEntityManager();

  public void startBuild(String renjinVersion) {

    RenjinCommit commit = entityManager.createQuery(
        "select c from RenjinCommit c where c.version = :v", RenjinCommit.class)
        .setParameter("v", renjinVersion)
        .getSingleResult();

    entityManager.getTransaction().begin();

    try {
      Build build = new Build();
      build.setRenjinCommit(commit);
      build.setStarted(new Date());
      entityManager.persist(build);

      List<Tuple> versions = entityManager.createQuery(
          "select v.id, count(d.id) from RPackageVersion v LEFT JOIN v.dependencies d GROUP BY v.id order by count(v.id) asc", Tuple.class)
          .setMaxResults(20)
          .getResultList();

      for(Tuple version : versions) {

        String versionId = (String) version.get(0);
        long numDeps = version.get(1, Long.class);

        RPackageBuild packageBuild = new RPackageBuild();
        packageBuild.setBuild(build);
        packageBuild.setPackageVersion(entityManager.getReference(RPackageVersion.class, versionId));
        packageBuild.setDependenciesResolved(numDeps == 0);
        entityManager.persist(packageBuild);
      }
      entityManager.getTransaction().commit();

    } catch(Exception e) {
      entityManager.getTransaction().rollback();
      throw new RuntimeException(e);
    }
    entityManager.close();
  }

  public static void main(String[] args) {
    new BuildLauncher().startBuild("0.7.0-RC7");
  }
}
