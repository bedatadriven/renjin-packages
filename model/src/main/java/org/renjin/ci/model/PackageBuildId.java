package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identifies a build by packageId:renjinVersion:buildNumber
 */
public class PackageBuildId {

  private final PackageVersionId packageVersionId;
  private final long buildNumber;


  public PackageBuildId(String id) {
    String parts[] = id.split(":");
    this.packageVersionId = new PackageVersionId(parts[0], parts[1], parts[2]);
    this.buildNumber = Long.parseLong(parts[3]);
  }

  public PackageBuildId(PackageVersionId packageVersionId, long buildNumber) {
    this.packageVersionId = packageVersionId;
    this.buildNumber = buildNumber;
  }
  
  public PackageId getPackageId() {
    return packageVersionId.getPackageId();
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public String getGroupId() {
    return packageVersionId.getGroupId();
  }
  
  public long getBuildNumber() {
    return buildNumber;
  }

  public String getBuildVersion() {
    return packageVersionId.getVersionString() + "-b" + buildNumber;
  }
  
  public String getPackageName() {
    return packageVersionId.getPackageName();
  }

  @Override
  @JsonValue
  public String toString() {
    return packageVersionId + ":" + buildNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PackageBuildId that = (PackageBuildId) o;

    if (buildNumber != that.buildNumber) return false;
    if (!packageVersionId.equals(that.packageVersionId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = packageVersionId.hashCode();
    result = 31 * result + (int) (buildNumber ^ (buildNumber >>> 32));
    return result;
  }

}
