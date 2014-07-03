package org.renjin.ci.task;


import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;

public class PackageBuildResult {

  private String id;

  private BuildOutcome outcome;

  private NativeOutcome nativeOutcome;

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

  public void setNativeOutcome(NativeOutcome nativeOutcome) {
    this.nativeOutcome = nativeOutcome;
  }

  public NativeOutcome getNativeOutcome() {
    return nativeOutcome;
  }
}
