package org.renjin.build.agent.build;

import org.renjin.build.model.BuildOutcome;

public class BuildResult {

  private String packageVersionId;
  private BuildOutcome outcome;

  public BuildResult() {
  }

  public BuildResult(String packageVersionId, BuildOutcome outcome) {
    this.packageVersionId = packageVersionId;
    this.outcome = outcome;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }  
}
