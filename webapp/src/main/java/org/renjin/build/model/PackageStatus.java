package org.renjin.build.model;

import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfEmpty;
import com.googlecode.objectify.condition.IfNull;

import java.util.Set;

/**
 * Tracks the status of a PackageVersion for
 * a given RenjinVersion
 */
@Entity
public class PackageStatus {


  public static Key<PackageStatus> key(PackageVersionId packageVersionId, RenjinVersionId versionId) {
    return Key.create(PackageStatus.class, packageVersionId.toString() + ":" + versionId.toString());
  }

  /**
   * Composite key of packageVersionId:renjinVersion
   */
  @Id
  private String id;

  private BuildStatus buildStatus;

  /**
   * The set of compile time dependencies that have not yet
   * been resolved.
   */
  @Index
  @IgnoreSave(IfEmpty.class)
  private Set<PackageVersionId> blockingDependencies = Sets.newHashSet();

  @Unindex
  @IgnoreSave(IfEmpty.class)
  private Set<PackageBuildId> dependencies = Sets.newHashSet();

  @IgnoreSave(IfNull.class)
  private PackageBuildId build;

  public PackageStatus() {
  }

  public PackageStatus(PackageVersionId packageVersionId, RenjinVersionId renjinVersion) {
    this.id = key(packageVersionId, renjinVersion).getName();
  }

  public String getId() {
    return id;
  }

  public PackageVersionId getPackageVersionId() {
    String[] parts = id.split(":");
    return new PackageVersionId(parts[0], parts[1], parts[2]);
  }

  public RenjinVersionId getRenjinVersionId() {
    String[] parts = id.split(":");
    return new RenjinVersionId(parts[3]);
  }

  public void setId(String id) {
    this.id = id;
  }

  public BuildStatus getBuildStatus() {
    return buildStatus;
  }

  public void setBuildStatus(BuildStatus buildStatus) {
    this.buildStatus = buildStatus;
  }

  public Set<PackageVersionId> getBlockingDependencies() {
    return blockingDependencies;
  }

  public void setBlockingDependencies(Set<PackageVersionId> blockingDependencies) {
    this.blockingDependencies = blockingDependencies;
  }

  public Set<PackageBuildId> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<PackageBuildId> dependencies) {
    this.dependencies = dependencies;
  }

  public PackageBuildId getBuild() {
    return build;
  }

  public void setBuild(PackageBuildId build) {
    this.build = build;
  }
}
