package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.Serializable;

/**
 * PackageVersion identifier, a composite of groupId, packageName,
 * and version (a typical GAV from Maven world)
 */
public class PackageVersionId implements Serializable, Comparable<PackageVersionId> {
  private String groupId;
  private String packageName;
  private String version;

  PackageVersionId() {
  }

  @JsonCreator
  public PackageVersionId(String groupArtifactVersion) {
    String gav[] = groupArtifactVersion.split(":");
    this.groupId = gav[0];
    this.packageName = gav[1];
    this.version = gav[2];
  }

  public PackageVersionId(String groupId, String packageName, String version) {
    this.groupId = groupId;
    this.packageName = packageName;
    this.version = version;
  }

  public PackageVersionId(PackageId packageId, String version) {
    this.groupId = packageId.getGroupId();
    this.packageName = packageId.getPackageName();
    this.version = version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getVersionString() {
    return version;
  }

  public ArtifactVersion getVersion() {
    return new DefaultArtifactVersion(version);
  }

  public static PackageVersionId fromTriplet(String id) {
    String gav[] = id.split(":");
    return new PackageVersionId(gav[0], gav[1], gav[2]);
  }
  
  public boolean isNewer(PackageVersionId other) {
    return compareTo(other) > 0;
  }

  public String getPath() {
    return "/package/" + getGroupId() + "/" + getPackageName() + "/" + getVersionString();
  }
  
  @JsonValue
  @Override
  public String toString() {
    return groupId + ":" + packageName + ":" + version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PackageVersionId that = (PackageVersionId) o;

    if (!groupId.equals(that.groupId)) return false;
    if (!packageName.equals(that.packageName)) return false;
    if (!version.equals(that.version)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + packageName.hashCode();
    result = 31 * result + version.hashCode();
    return result;
  }
  
  @Override
  public int compareTo(PackageVersionId o) {
    if(!groupId.equals(o.groupId)) {
      return groupId.compareTo(o.groupId);
    }
    if(!packageName.equals(o.packageName)) {
      return packageName.compareTo(o.packageName);
    }
    return getVersion().compareTo(o.getVersion());
  }

  public PackageId getPackageId() {
    return new PackageId(groupId, packageName);
  }
}
