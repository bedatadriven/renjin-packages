package org.renjin.build.model;

/**
 * Identifies a build by packageId:renjinVersion:buildNumber
 */
public class PackageBuildId {

  private final PackageVersionId packageVersionId;
  private final RenjinVersionId renjinVersionId;
  private final long buildNumber;


  public PackageBuildId(String id) {
    String parts[] = id.split(":");
    this.packageVersionId = new PackageVersionId(parts[0], parts[1], parts[2]);
    this.renjinVersionId = new RenjinVersionId(parts[3]);
    this.buildNumber = Long.parseLong(parts[4]);
  }

  public PackageBuildId(PackageVersionId packageVersionId, RenjinVersionId renjinVersionId, long buildNumber) {
    this.packageVersionId = packageVersionId;
    this.renjinVersionId = renjinVersionId;
    this.buildNumber = buildNumber;
  }

  @Override
  public String toString() {
    return packageVersionId + ":" + renjinVersionId + ":" + buildNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PackageBuildId that = (PackageBuildId) o;

    if (buildNumber != that.buildNumber) return false;
    if (!packageVersionId.equals(that.packageVersionId)) return false;
    if (!renjinVersionId.equals(that.renjinVersionId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = packageVersionId.hashCode();
    result = 31 * result + renjinVersionId.hashCode();
    result = 31 * result + (int) (buildNumber ^ (buildNumber >>> 32));
    return result;
  }
}
