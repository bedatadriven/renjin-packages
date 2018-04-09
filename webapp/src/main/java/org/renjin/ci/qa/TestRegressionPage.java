package org.renjin.ci.qa;

import com.google.common.base.Strings;
import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.releases.ReleasesResource;


public class TestRegressionPage {
  private final TestRegression regression;
  private LoadResult<PackageTestResult> brokenResult;
  private LoadResult<PackageTestResult> lastGoodResult;
  private LoadResult<PackageTestResult> latestResult;
  private final PackageBuildId lastBuildId;
  private final PackageVersion packageVersion;

  public TestRegressionPage(TestRegression regression) {
    this.regression = regression;
    this.brokenResult = PackageDatabase.getTestResult(regression.getBrokenBuildId(), regression.getTestName());
    this.lastGoodResult = PackageDatabase.getTestResult(regression.getLastGoodBuildId(), regression.getTestName());
    packageVersion = PackageDatabase.getPackageVersion(getPackageVersionId()).get();
    lastBuildId = packageVersion.getLastBuildId();
    this.latestResult = PackageDatabase.getTestResult(lastBuildId, regression.getTestName());
  }

  public String getSummary() {
    return Strings.nullToEmpty(regression.getSummary());
  }

  public String getSourceUrl() {
    PackageVersionId pvid = getPackageVersionId();

    switch (pvid.getGroupId()) {
      case "org.renjin.cran":
        return "https://github.com/cran/" + pvid.getPackageName() +
            "/tree/" + pvid.getVersionString();

      case "org.renjin.bioconductor":
        return "https://github.com/bioconductor/" + pvid.getPackageName();

        default:
        return null;
    }
  }

  public PackageVersionId getPackageVersionId() {
    return regression.getPackageVersionId();
  }

  public PackageId getPackageId() {
    return regression.getPackageId();
  }

  public String getTestName() {
    return regression.getTestName();
  }

  public TestRegressionStatus getStatus() {
    if(regression.getStatus() == null) {
      return TestRegressionStatus.UNCONFIRMED;
    }
    return regression.getStatus();
  }

  public String getTestHistoryPath() {
    return getPackageVersionId().getPath() + "/test/" + getTestName() + "/history";
  }

  public boolean isNewerResult() {
    if(latestResult.now() == null) {
      return false;
    }
    return !lastBuildId.equals(regression.getBrokenBuildId());
  }

  public PackageTestResult getBrokenResult() {
    return brokenResult.now();
  }

  public PackageTestResult getLatestResult() {
    return latestResult.now();
  }

  public PackageTestResult getLastGoodResult() {
    return lastGoodResult.now();
  }

  public String getComparePath() {
    return ReleasesResource.compareUrl(lastGoodResult.now().getRenjinVersionId(), brokenResult.now().getRenjinVersionId());
  }

  public String getDetailPath() {
    return regression.getPath();
  }

  public String getUpdatePath() {
    return getDetailPath() + "/update";
  }

  public String getNextPath() {
    return getDetailPath() + "/next";
  }

  public String getMarkFormPath() {
    return "/qa/markTestResults?packageId=" + getPackageVersionId().getPackageId() + "&testName=" + getTestName();
  }
}
