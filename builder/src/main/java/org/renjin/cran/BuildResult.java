package org.renjin.cran;

import org.renjin.repo.model.BuildOutcome;

public class BuildResult {

  private String packageVersionId;
  private BuildOutcome outcome;

  public BuildResult() {
    
  }
  
  public BuildResult(String packageVersionId, BuildOutcome outcome) {
    this.packageVersionId = name;
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
