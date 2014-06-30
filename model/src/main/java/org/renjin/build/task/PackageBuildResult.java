package org.renjin.build.task;


import org.renjin.build.model.BuildOutcome;

public class PackageBuildResult {

  private String id;

  private BuildOutcome outcome;

  private boolean nativeSourcesCompilationFailure;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  public boolean isNativeSourcesCompilationFailure() {
    return nativeSourcesCompilationFailure;
  }

  public void setNativeSourcesCompilationFailure(boolean nativeSourcesCompilationFailure) {
    this.nativeSourcesCompilationFailure = nativeSourcesCompilationFailure;
  }

}
