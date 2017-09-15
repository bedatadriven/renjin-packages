package org.renjin.ci.model;


import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class PackageBuildResult {

  private String id;

  private String packageVersionId;

  private BuildOutcome outcome;

  private NativeOutcome nativeOutcome;
  
  private List<String> blockingDependencies;

  private List<String> resolvedDependencies;
  
  private List<TestResult> testResults;

  /**
   * The GIT commit id of the patched version of this package used.
   */
  private String patchId;

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

  public String getPackageVersionId() {
    return packageVersionId;
  }

  @JsonSetter
  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public void setPackageVersionId(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId.toString();
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

  public List<TestResult> getTestResults() {
    return testResults;
  }

  public void setTestResults(List<TestResult> testResults) {
    this.testResults = testResults;
  }

  public String getPatchId() {
    return patchId;
  }

  public void setPatchId(String patchId) {
    this.patchId = patchId;
  }
}
