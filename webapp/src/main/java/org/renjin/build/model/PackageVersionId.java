package org.renjin.build.model;

/**
 * PackageVersion identifier, a composite of groupId, packageName,
 * and sourceVersion (a typical GAV from Maven world)
 */
public class PackageVersionId {
  private String groupId;
  private String packageName;
  private String sourceVersion;

  PackageVersionId() {
  }


  public PackageVersionId(String groupArtifactVersion) {
    String gav[] = groupArtifactVersion.split(":");
    this.groupId = gav[0];
    this.packageName = gav[1];
    this.sourceVersion = gav[2];
  }

  public PackageVersionId(String groupId, String packageName, String sourceVersion) {
    this.groupId = groupId;
    this.packageName = packageName;
    this.sourceVersion = sourceVersion;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getSourceVersion() {
    return sourceVersion;
  }

  public static PackageVersionId fromTriplet(String id) {
    String gav[] = id.split(":");
    return new PackageVersionId(gav[0], gav[1], gav[2]);
  }

  @Override
  public String toString() {
    return groupId + ":" + packageName + ":" + sourceVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PackageVersionId that = (PackageVersionId) o;

    if (!groupId.equals(that.groupId)) return false;
    if (!packageName.equals(that.packageName)) return false;
    if (!sourceVersion.equals(that.sourceVersion)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + packageName.hashCode();
    result = 31 * result + sourceVersion.hashCode();
    return result;
  }
}
