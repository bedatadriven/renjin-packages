package org.renjin.cran;


import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.util.Date;

public class BuildReporter {

  private final int id;

  public BuildReporter(Workspace workspace) {
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
}
