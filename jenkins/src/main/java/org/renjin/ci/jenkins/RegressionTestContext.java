package org.renjin.ci.jenkins;

import hudson.FilePath;
import org.renjin.ci.model.PullBuildId;
import org.renjin.ci.model.RenjinVersionId;

import java.io.IOException;

public class RegressionTestContext {
  private RenjinVersionId renjinVersionId;
  private WorkerContext workerContext;
  private PullBuildId pullBuildId;
  private final FilePath localRepo;

  public RegressionTestContext(PullBuildId pullBuildId, WorkerContext workerContext) throws IOException, InterruptedException {
    this.pullBuildId = pullBuildId;
    this.workerContext = workerContext;
    this.localRepo = workerContext.getWorkspace().child("m2-" + pullBuildId.getBuildNumber());
    if(this.localRepo.exists()) {
      this.localRepo.mkdirs();
    }
  }

  public WorkerContext getWorkerContext() {
    return workerContext;
  }

  public RenjinVersionId getRenjinVersionId() {
    return pullBuildId.getRenjinVersionId();
  }

  public String getBuildNumber() {
    return pullBuildId.getBuildNumber();
  }

  public PullBuildId getPullBuildId() {
    return pullBuildId;
  }

  public String getLocalMavenRepo() {
    return this.localRepo.getRemote();
  }
}
