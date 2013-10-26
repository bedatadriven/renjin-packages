package org.renjin.cran;


import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RPackageBuildResult;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

public class BuildReporter {

  private final int id;
  private final Workspace workspace;

  public BuildReporter(Workspace workspace) {
    this.workspace = workspace;

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();

    RenjinCommit commit = em.find(RenjinCommit.class, workspace.getRenjinCommitId());
    if(commit == null) {
      commit = new RenjinCommit();
      commit.setId(workspace.getRenjinCommitId());
      commit.setVersion(workspace.getRenjinVersion());
      em.persist(commit);
    }

    Build build = new Build();
    build.setStarted(new Date());
    build.setRenjinCommit(commit);
    em.persist(build);
    em.getTransaction().commit();
    em.close();
    this.id = build.getId();
  }

  public void reportResult(PackageNode pkg, BuildOutcome outcome) {
    new BuildResultRecorder(id, pkg, outcome).record();
  }

  public boolean packageAlreadySucceeded(String packageVersion) {

    EntityManager em = PersistenceUtil.createEntityManager();
    List<RPackageBuildResult> results = em.createQuery("select r from RPackageBuildResult r where r.packageVersion.id = :packageVersion and " +
      "r.build.renjinCommit.id = :commit", RPackageBuildResult.class)
      .setParameter("packageVersion", packageVersion)
      .setParameter("commit", workspace.getRenjinCommitId())
      .getResultList();

    boolean succeeded = false;
    for(RPackageBuildResult result : results) {
      if(result.getOutcome() == BuildOutcome.SUCCESS) {
        succeeded = true;
        break;
      }
    }

    em.close();

    return succeeded;
  }
}
