package org.renjin.build.task;

import java.util.List;

public class PackageBuildTask {

  /**
   * Package groupId:artifactId:versionId
   */
  private String packageGroupId;

  private String packageName;

  private String packageVersion;

  private String renjinVersion;

  private int buildId;

  private List<String> dependencies;


  public String getPackageGroupId() {
    return packageGroupId;
  }

  public void setPackageGroupId(String packageGroupId) {
    this.packageGroupId = packageGroupId;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getPackageVersion() {
    return packageVersion;
  }

  public void setPackageVersion(String packageVersion) {
    this.packageVersion = packageVersion;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public int getBuildId() {
    return buildId;
  }

  public void setBuildId(int buildId) {
    this.buildId = buildId;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public String packageBuildId() {
    return packageGroupId + ":" + packageName + ":" + packageVersion + "-b" + buildId;
  }

  public String versionId() {
    return packageGroupId + ":" + packageName + ":" + packageVersion;
  }
}
