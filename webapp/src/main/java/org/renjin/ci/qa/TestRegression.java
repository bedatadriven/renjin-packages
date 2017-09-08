package org.renjin.ci.qa;

import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.releases.ReleasesResource;
import org.renjin.ci.storage.StorageKeys;


public class TestRegression {
  private PackageVersionId packageVersionId;
  private String testName;
  
  private PackageBuildId lastGoodBuild;
  private RenjinVersionId lastGoodRenjinVersion;
  private PackageBuildId brokenBuild;
  private RenjinVersionId brokenRenjinVersionId;

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public String getTestName() {
    return testName;
  }

  public void setTestName(String testName) {
    this.testName = testName;
  }
  
  public PackageBuildId getLastGoodBuild() {
    return lastGoodBuild;
  }

  public void setLastGoodBuild(PackageBuildId lastGoodBuild) {
    this.lastGoodBuild = lastGoodBuild;
  }

  public RenjinVersionId getLastGoodRenjinVersion() {
    return lastGoodRenjinVersion;
  }

  public void setLastGoodRenjinVersion(RenjinVersionId lastGoodRenjinVersion) {
    this.lastGoodRenjinVersion = lastGoodRenjinVersion;
  }
  
  
  public PackageBuildId getBrokenBuild() {
    return brokenBuild;
  }

  public void setBrokenBuild(PackageBuildId brokenBuild) {
    this.brokenBuild = brokenBuild;
  }

  public RenjinVersionId getBrokenRenjinVersionId() {
    return brokenRenjinVersionId;
  }
  
  public String getTestHistoryPath() {
    return packageVersionId.getPath() + "/test/" + testName + "/history";
  }

  public void setBrokenRenjinVersionId(RenjinVersionId brokenRenjinVersionId) {
    this.brokenRenjinVersionId = brokenRenjinVersionId;
  }

  public String getBrokenLogUrl() {
    return StorageKeys.testLogUrl(getBrokenBuild(), testName);
  }
  
  public String getLastGoodLogUrl() {
    return StorageKeys.testLogUrl(lastGoodBuild, testName);
  }
  
  public String getComparePath() {
    return ReleasesResource.compareUrl(lastGoodRenjinVersion, brokenRenjinVersionId);
  }

}
