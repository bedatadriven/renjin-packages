package org.renjin.ci.datastore;

import org.renjin.ci.model.PackageVersionId;

public class TestRegressionId {
  private PackageVersionId packageVersionId;
  private String testName;
  private long brokenBuildNumber;

  public TestRegressionId(PackageVersionId packageVersionId, String testName, long brokenBuildNumber) {
    this.packageVersionId = packageVersionId;
    this.testName = testName;
    this.brokenBuildNumber = brokenBuildNumber;
  }

  public String getPath() {
    return "/qa/testRegression/" +
        packageVersionId.getGroupId() + "/" +
        packageVersionId.getPackageName() + "/" +
        packageVersionId.getVersion() + "/" +
        testName + "/" +
        brokenBuildNumber;
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public String getTestName() {
    return testName;
  }

  public long getBrokenBuildNumber() {
    return brokenBuildNumber;
  }
}
