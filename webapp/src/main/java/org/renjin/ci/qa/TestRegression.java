package org.renjin.ci.qa;

import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.releases.ReleasesResource;


public class TestRegression {
  private PackageVersionId packageVersionId;
  private String testName;
  private final BuildDelta buildDelta;
  private final PackageBuildId brokenBuild;

  private final Iterable<PackageTestResult> testResults;
  private PackageTestResult brokenResult;
  private PackageTestResult latestResult;
  private PackageTestResult lastGoodResult;


  public TestRegression(PackageVersionDelta delta, String testName) {
    this.packageVersionId = delta.getPackageVersionId();
    this.testName = testName;
    this.buildDelta = findBuildWithRegression(delta, testName);
    this.brokenBuild = new PackageBuildId(delta.getPackageVersionId(), buildDelta.getBuildNumber());
    this.testResults = PackageDatabase.getTestResults(packageVersionId, testName);

    // find the broken test result,
    // the most recent good build
    for (PackageTestResult testResult : testResults) {
      if (testResult.getBuildId().equals(brokenBuild)) {
        brokenResult = testResult;
      }
      if (testResult.isPassed()) {
        if(lastGoodResult == null || testResult.isNewerThan(lastGoodResult)) {
          lastGoodResult = testResult;
        }
      }
      if(latestResult == null || testResult.isNewerThan(latestResult)) {
        latestResult = testResult;
      }
    }
  }

  public String getTestId() {
    return packageVersionId.toString() + ":" + testName;
  }

  public String getSourceUrl() {
    if(packageVersionId.getGroupId().equals("org.renjin.cran")) {
      return "https://github.com/cran/" + packageVersionId.getPackageName() +
          "/tree/" + packageVersionId.getVersionString();
    } else if(packageVersionId.getGroupId().equals("org.renjin.bioconductor")) {
      return "https://github.com/bioconductor/" + packageVersionId.getPackageName();
    } else {
      return null;
    }
  }

  private static BuildDelta findBuildWithRegression(PackageVersionDelta versionDelta, String testName) {
    for (BuildDelta buildDelta : versionDelta.getBuilds()) {
      if(buildDelta.getTestRegressions().contains(testName)) {
        return buildDelta;
      }
    }
    throw new IllegalStateException();
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

  public String getTestHistoryPath() {
    return packageVersionId.getPath() + "/test/" + testName + "/history";
  }

  public boolean isNewerResult() {
    return latestResult != brokenResult;
  }

  public PackageTestResult getBrokenResult() {
    return brokenResult;
  }

  public PackageTestResult getLatestResult() {
    return latestResult;
  }

  public PackageTestResult getLastGoodResult() {
    return lastGoodResult;
  }

  public String getComparePath() {
    return ReleasesResource.compareUrl(lastGoodResult.getRenjinVersionId(), brokenResult.getRenjinVersionId());
  }

  public String getDetailPath() {
    return "/qa/testRegression/" + getPackageVersionId().getGroupId() + "/" + getPackageId().getPackageName() + "/" +
        getPackageVersionId().getVersionString() + "/" + testName;
  }

  public String getMarkFormPath() {
    return "/qa/markTestResults?packageId=" + getPackageVersionId().getPackageId() + "&testName=" + testName;
  }
}
