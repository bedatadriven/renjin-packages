package org.renjin.build.task;

import java.util.List;

public class PackageBuildTask {

  private String groupId;
  private String packageName;
  private String sourceVersion;
  private String buildNumber;

  private String pom;
  private String sourceUrl;

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getSourceVersion() {
    return sourceVersion;
  }

  public void setSourceVersion(String sourceVersion) {
    this.sourceVersion = sourceVersion;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
  }

  public String getPom() {
    return pom;
  }

  public void setPom(String pom) {
    this.pom = pom;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  @Override
  public String toString() {
    return groupId + ":" + packageName + ":" + sourceVersion + "-b" + buildNumber;
  }
}
