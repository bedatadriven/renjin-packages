package org.renjin.build.agent.build;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.agent.workspace.Workspace;
import org.renjin.build.model.RPackageBuild;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Program that will retrieve package sources from CRAN,
 * build, and report results.
 */
public class BuildCommand  {

  public static final String WORKER_ID = "XYZ";


  public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
    new BuildCommand().run();
  }

  public void run() throws IOException, GitAPIException, InterruptedException {
    Workspace workspace = new Workspace(new File("/tmp/workspace"));

    while(true) {
      RPackageBuild build = leaseNextBuild();
      if(build == null) {
        System.out.println("Nothing to do...");
        Thread.sleep(5_000);
      } else {
        new PackageBuilder(workspace, build).build();
      }
    }
  }

  public RPackageBuild leaseNextBuild() {
    EntityManager em = PersistenceUtil.createEntityManager();

    em.getTransaction().begin();
    try {
      List<RPackageBuild> resultList = em.createQuery(
          "select b from RPackageBuild b " +
              "where dependenciesResolved = true and outcome = null and leased = null", RPackageBuild.class)
          .setMaxResults(1)
          .getResultList();

      RPackageBuild build;
      if(!resultList.isEmpty()) {
        build = resultList.get(0);
        build.setLeased(WORKER_ID);
        build.setLeaseTime(new Date());
      } else {
        // none available
        build = null;
      }
      em.getTransaction().commit();

      return build;
    } catch(Exception e) {
      em.getTransaction().rollback();
      throw new RuntimeException("Failed to lease task");
    }
  }
}
