package org.renjin.build.worker;


import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.BuildStage;
import org.renjin.build.model.RPackageBuild;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static Logger LOGGER = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) throws IOException, InterruptedException {
    File workspace = new File("/tmp/workspace/packages");

    while(true) {
      Optional<RPackageBuild> packageToBuild = leaseNextBuild();
      if(packageToBuild == null) {
        LOGGER.info("No tasks available, sleeping 60s...");
        Thread.sleep(60_000);
      } else {

        try {
          // wrap up last task:
          new PackageBuilder(workspace, packageToBuild.get()).build();

          LOGGER.log(Level.INFO, packageToBuild.get().getId() + ": " +
              packageToBuild.get().getOutcome());

          // keep track of how it went
          completeTask(packageToBuild);

        } catch(Exception e) {
          LOGGER.log(Level.SEVERE, "Exception while building " + packageToBuild.get().getPackageName(), e);
        }
      }
    }
  }


  public static Optional<RPackageBuild> leaseNextBuild() {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
    try {
      List<RPackageBuild> resultList = em.createQuery(
          "select b from RPackageBuild b " +
              "where stage='READY'", RPackageBuild.class)
          .setMaxResults(1)
          .getResultList();

      RPackageBuild build;
      if(!resultList.isEmpty()) {
        build = resultList.get(0);
        build.setLeased(getWorkerId());
        build.setLeaseTime(new Date());
        build.setStage(BuildStage.LEASED);
      } else {
        // none available
        return Optional.absent();
      }
      em.getTransaction().commit();

      return Optional.of(build);


    } catch(Exception e) {
      em.getTransaction().rollback();
      LOGGER.log(Level.SEVERE, "Exception while leasing build...");
      return Optional.absent();

    } finally {
      try {
        em.close();
      } catch(Exception e) {
        LOGGER.log(Level.SEVERE, "Exception while closing EntityManager", e);
      }
    }
  }

  private static void completeTask(Optional<RPackageBuild> packageBuild) {
    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
    try {

      RPackageBuild update = em.find(RPackageBuild.class, packageBuild.get().getId());

      // make sure our lease hasn't been revoked
      if(!getWorkerId().equals(update.getLeased())) {
        LOGGER.log(Level.WARNING, "Leased on build " + packageBuild.get().getId() +
            " was revoked before we completed; not reporting results.");
      } else {
        update.setStage(BuildStage.COMPLETED);
        update.setOutcome(packageBuild.get().getOutcome());
        update.setNativeSourceCompilationFailures(packageBuild.get().isNativeSourceCompilationFailures());
        update.setCompletionTime(new Date());
        em.persist(update);
      }
      em.getTransaction().commit();

    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while reporting results on " + packageBuild.get().getPackageName(), e);

      try {
        em.getTransaction().rollback();
      } catch(Exception rollbackException) {
        LOGGER.log(Level.SEVERE, "Exception while rolling back", rollbackException);
      }

    } finally {
      try {
        em.close();
      } catch(Exception e) {
        LOGGER.log(Level.SEVERE, "Exception while closing EntityManager", e);
      }
    }
  }


  private static String getWorkerId() {
    String id = System.getenv("HOSTNAME");
    if(Strings.isNullOrEmpty(id)) {
      id = "worker" + Long.toString(Thread.currentThread().getId());
    }
    return id;
  }
}
