package org.renjin.infra.agent.build;


import com.google.common.io.Files;
import org.renjin.infra.agent.util.GoogleCloudStorage;
import org.renjin.infra.agent.workspace.Workspace;
import org.renjin.repo.PersistenceUtil;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.io.File;
import java.util.Date;
import java.util.List;

public class BuildReporter {

  private int buildId;

  private List<String> successfullyBuiltPackageVersions;
  private Workspace workspace;

  public BuildReporter(Workspace workspace) {
    this.workspace = workspace;

    if(!workspace.isDevMode()) {
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
    }
  }

  public void reportResult(PackageNode pkg, BuildOutcome outcome, File baseDir, File logFile) {
    if(!workspace.isDevMode()) {
      new BuildResultRecorder(buildId, pkg, baseDir, outcome, logFile).record();

      try {
        GoogleCloudStorage.INSTANCE.putBuildLog(buildId, pkg.getId(),
          Files.newInputStreamSupplier(logFile));
      } catch(Exception e) {
        System.err.println("Failed to post build log: " + e.getMessage());
      }
    }
  }

  public boolean packageAlreadySucceeded(String packageVersion) {
    if(workspace.isDevMode()) {
      return false;
    } else {
      return successfullyBuiltPackageVersions.contains(packageVersion);
    }
  }

  public int getBuildId() {
    return buildId;
  }
}
