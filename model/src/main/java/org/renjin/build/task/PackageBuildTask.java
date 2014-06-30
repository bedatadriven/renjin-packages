package org.renjin.build.task;

public class PackageBuildTask {

  private String packageVersionId;
  private long buildNumber;
  private String pom;

  public String url() {
    return "https://renjinpackages.appspot.com/build/" + packageVersionId.replace(':', '/') + "/" + buildNumber;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public long getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(long buildNumber) {
    this.buildNumber = buildNumber;
  }

  public String getPom() {
    return pom;
  }

  public void setPom(String pom) {
    this.pom = pom;
  }

  @Override
  public String toString() {
    return packageVersionId + "-b" + buildNumber;
  }


}
