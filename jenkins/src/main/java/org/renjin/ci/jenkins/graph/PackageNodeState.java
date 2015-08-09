package org.renjin.ci.jenkins.graph;

import org.renjin.ci.model.BuildOutcome;


public class PackageNodeState {

  public static final PackageNodeState NOT_BUILT = new PackageNodeState(-1, null);
  public static final PackageNodeState ORPHANED = new PackageNodeState(-1, BuildOutcome.FAILURE);
  public static final PackageNodeState CANCELLED = new PackageNodeState(-1, BuildOutcome.CANCELLED);

  private final long buildNumber;
  private final BuildOutcome outcome;

  public PackageNodeState(long buildNumber, BuildOutcome outcome) {
    this.buildNumber = buildNumber;
    this.outcome = outcome;
  }

  public static PackageNodeState success(long buildNumber) {
    return new PackageNodeState(buildNumber, BuildOutcome.SUCCESS);
  }

  @Override
  public String toString() {
    if(this == NOT_BUILT) {
      return "not_built";
    } else if(outcome == BuildOutcome.SUCCESS) {
      return "SUCCESS #" + buildNumber;
    } else {
      return outcome.toString();
    }
  }

  public boolean isBuilt() {
    return outcome == BuildOutcome.SUCCESS;
  }

  public long getBuildNumber() {
    return buildNumber;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public static PackageNodeState error() {
    return new PackageNodeState(-1, BuildOutcome.ERROR);
  }

  public static PackageNodeState build(long buildNumber, BuildOutcome outcome) {
    return new PackageNodeState(buildNumber, outcome);
  }
}
