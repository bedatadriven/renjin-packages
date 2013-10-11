package org.renjin.cran;


import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;

import javax.persistence.EntityManager;
import java.util.Date;

public class BuildReporter {

  private final int id;

  public BuildReporter(Workspace workspace) {
    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
    Build build = new Build();
    build.setStarted(new Date());
    build.setRenjinCommitId(workspace.getRenjinCommitId());
    build.setRenjinVersion(workspace.getRenjinVersion());
    em.persist(build);
    em.getTransaction().commit();
    em.close();
    this.id = build.getId();
  }

  public void reportResult(PackageNode pkg, BuildOutcome outcome) {
    new BuildResultRecorder(id, pkg, outcome).record();
  }
}
