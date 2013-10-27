package org.renjin.cran;


import com.google.common.io.Files;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

public class BuildReporter {

  private final int buildId;

  private BuildLogUploader logUploader;

  private List<String> successfullyBuiltPackageVersions;

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

    // get list of packages which have already succeeded in this commit
    successfullyBuiltPackageVersions = em.createQuery(
      "select r.packageVersion.id from RPackageBuildResult r " +
        "where r.build.renjinCommit.id = :commit and r.outcome=:outcome", String.class)
      .setParameter("commit", workspace.getRenjinCommitId())
      .setParameter("outcome", BuildOutcome.SUCCESS)
      .getResultList();

    em.getTransaction().commit();
    em.close();
    this.buildId = build.getId();

    logUploader = new BuildLogUploader(buildId);
  }

  public void reportResult(PackageNode pkg, BuildOutcome outcome) {
    new BuildResultRecorder(buildId, pkg, outcome).record();

    try {
      logUploader.put(pkg.getPackageVersionId(), Files.newInputStreamSupplier(pkg.getLogFile()));
    } catch(Exception e) {
      System.err.println("Failed to post build log: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public boolean packageAlreadySucceeded(String packageVersion) {
    return successfullyBuiltPackageVersions.contains(packageVersion);
  }
}
