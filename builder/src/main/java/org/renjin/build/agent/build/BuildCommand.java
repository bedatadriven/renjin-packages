package org.renjin.build.agent.build;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.agent.workspace.Workspace;
import org.renjin.build.model.BuildStage;
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

  private EntityManager em = PersistenceUtil.createEntityManager();


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

        // wrap up last task:
        new PackageBuilder(workspace, build).build();

        // keep track of how it went
        em.getTransaction().begin();
        em.persist(build);
        em.getTransaction().commit();

      }
    }
  }

  public RPackageBuild leaseNextBuild() {

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
        build.setLeased(WORKER_ID);
        build.setLeaseTime(new Date());
        build.setStage(BuildStage.LEASED);
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
