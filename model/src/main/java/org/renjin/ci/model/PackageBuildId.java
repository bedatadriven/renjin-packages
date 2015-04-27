package org.renjin.ci.model;

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

  public long getBuildNumber() {
    return buildNumber;
  }

  @Override
  public String toString() {
    return packageVersionId + "-b" + buildNumber;
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
