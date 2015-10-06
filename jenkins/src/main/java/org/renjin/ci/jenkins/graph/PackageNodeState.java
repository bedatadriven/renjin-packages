package org.renjin.ci.jenkins.graph;

import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;


public class PackageNodeState {

  public static final PackageNodeState NOT_BUILT = new PackageNodeState(null, null);
  public static final PackageNodeState ORPHANED = new PackageNodeState(null, BuildOutcome.FAILURE);
  public static final PackageNodeState CANCELLED = new PackageNodeState(null, BuildOutcome.CANCELLED);
  public static final PackageNodeState ERROR = new PackageNodeState(null, BuildOutcome.ERROR);

  private final String buildVersion;
  private final BuildOutcome outcome;

  public PackageNodeState(String buildVersion, BuildOutcome outcome) {
    this.outcome = outcome;
    this.buildVersion = buildVersion;
  }
  
  public PackageNodeState(PackageVersionId versionId, long buildNumber, BuildOutcome outcome) {
    this(new PackageBuildId(versionId, buildNumber).getBuildVersion(), outcome);
  }

  @Override
  public String toString() {
    if(this == NOT_BUILT) {
      return "not_built";
    } else if(outcome == BuildOutcome.SUCCESS) {
      return "SUCCESS #" + buildVersion;
    } else {
      return outcome.toString();
    }
  }

  public boolean isBuilt() {
    return buildVersion != null;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public static PackageNodeState build(PackageVersionId versionId, long buildNumber, BuildOutcome outcome) {
    return new PackageNodeState(versionId, buildNumber, outcome);
  }

  public String getBuildVersion() {
    return buildVersion;
  }
}
