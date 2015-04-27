package org.renjin.ci.packages.results;

import org.renjin.ci.model.PackageTestResult;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.ArrayList;
import java.util.List;


public class TestRun {
  private long testRunNumber;
  private RenjinVersionId renjinVersion;
  private int passCount;
  private final PackageVersionId packageVersionId;
  private long packageBuildNumber;
  private List<PackageTestResult> testResults = new ArrayList<>();

  public TestRun(long testRunNumber, RenjinVersionId renjinVersion, PackageVersionId pvid, long packageBuildNumber) {
    this.testRunNumber = testRunNumber;
    this.renjinVersion = renjinVersion;
    this.packageVersionId = pvid;
    this.packageBuildNumber = packageBuildNumber;
  }
  
  public void add(PackageTestResult result) {
    testResults.add(result);
    if(result.isPassed()) {
      passCount++;
    }
  }

  public long getTestRunNumber() {
    return testRunNumber;
  }
  
  public String getPackageVersion() {
    return packageVersionId.getVersionString();
  }

  public long getBuildNumber() {
    return packageBuildNumber;
  }

  public String getRenjinVersion() {
    return renjinVersion.toString();
  }

  public int getPassCount() {
    return passCount;
  }
  
  public int getCount() {
    return testResults.size();
  }

  public List<PackageTestResult> getTestResults() {
    return testResults;
  }
}
