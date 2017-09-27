package org.renjin.ci.qa;

import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

public class TestRegressionEntry {
  private final PackageVersionId packageVersionId;
  private final BuildDelta buildDelta;
  private final String testName;
  private final PackageBuildId brokenBuild;
  private final RenjinVersionId brokenRenjinVersionId;


  public TestRegressionEntry(PackageVersionDelta delta, BuildDelta buildDelta, String testName) {
    this.packageVersionId = delta.getPackageVersionId();
    this.buildDelta = buildDelta;
    this.testName = testName;
    this.brokenBuild = new PackageBuildId(delta.getPackageVersionId(), buildDelta.getBuildNumber());
    this.brokenRenjinVersionId = buildDelta.getRenjinVersionId();
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public PackageId getPackageId() {
    return packageVersionId.getPackageId();
  }

  public String getTestName() {
    return testName;
  }

  public PackageBuildId getBrokenBuild() {
    return brokenBuild;
  }

  public RenjinVersionId getBrokenRenjinVersionId() {
    return brokenRenjinVersionId;
  }


  public String getDetailPath() {
    return "/qa/testRegression/" +
        packageVersionId.getGroupId() + "/" +
        packageVersionId.getPackageName() + "/" +
        packageVersionId.getVersionString() + "/" +
        testName;
  }

}
