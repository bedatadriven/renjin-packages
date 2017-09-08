package org.renjin.ci.model;

/**
 * Identifies a specific build of a Renjin pull request.
 */
public class PullBuildId {

  private long pullNumber;
  private long pullBuildNumber;

  public PullBuildId(long pullNumber, long pullBuildNumber) {
    this.pullNumber = pullNumber;
    this.pullBuildNumber = pullBuildNumber;
  }

  public RenjinVersionId getRenjinVersionId() {
    return RenjinVersionId.valueOf("1000-" + getBuildNumber());
  }

  public String getBuildNumber() {
    return "pr" + pullNumber + "-b" + pullBuildNumber;
  }

  public long getPullNumber() {
    return pullNumber;
  }

  public long getPullBuildNumber() {
    return pullBuildNumber;
  }


}
