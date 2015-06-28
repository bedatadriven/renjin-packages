package org.renjin.ci.model;


import java.util.List;

public class PackageBuildResult {

  private String id;

  private BuildOutcome outcome;

  private NativeOutcome nativeOutcome;
  
  private List<String> blockingDependencies;
  
  private List<String> resolvedDependencies;

  private PackageBuildResult() {
  }

  public PackageBuildResult(BuildOutcome outcome, NativeOutcome nativeOutcome) {
    this.outcome = outcome;
    this.nativeOutcome = nativeOutcome;
  }

  public PackageBuildResult(BuildOutcome outcome) {
    this.outcome = outcome;
    this.nativeOutcome = NativeOutcome.NA;
  }

  public static PackageBuildResult timeout() {
    PackageBuildResult result = new PackageBuildResult();
    result.setOutcome(BuildOutcome.TIMEOUT);
    return result;
  }

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

  public List<String> getBlockingDependencies() {
    return blockingDependencies;
  }

  public void setBlockingDependencies(List<String> blockingDependencies) {
    this.blockingDependencies = blockingDependencies;
  }

  public List<String> getResolvedDependencies() {
    return resolvedDependencies;
  }

  public void setResolvedDependencies(List<String> resolvedDependencies) {
    this.resolvedDependencies = resolvedDependencies;
  }
}
