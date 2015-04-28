package org.renjin.ci.datastore;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfEmpty;
import com.googlecode.objectify.condition.IfZero;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.renjin.ci.model.*;

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

  private int compatibilityLevel;
  
  @Index
  private BuildStatus buildStatus;

  @Index
  @IgnoreSave(IfZero.class)
  private int buildDelta;

  @Unindex
  @IgnoreSave(IfZero.class)
  private long buildNumber;

  private Integer testCount;
  
  private Integer testPassCount;
  
  /**
   * The set of compile time dependencies that have not yet
   * been resolved.
   */
  @Index
  @IgnoreSave(IfEmpty.class)
  private Set<String> blockingDependencies = Sets.newHashSet();

  @Unindex
  @IgnoreSave(IfEmpty.class)
  private Set<String> dependencies = Sets.newHashSet();

  public PackageStatus() {
  }

  public PackageStatus(PackageVersionId packageVersionId, RenjinVersionId renjinVersion) {
    this.id = key(packageVersionId, renjinVersion).getName();
  }

  public String getId() {
    return id;
  }

  public int getCompatibilityLevel() {
    return compatibilityLevel;
  }

  public void setCompatibilityLevel(int compatibilityLevel) {
    this.compatibilityLevel = compatibilityLevel;
  }

  public PackageVersionId getPackageVersionId() {
    String[] parts = id.split(":");
    return new PackageVersionId(parts[0], parts[1], parts[2]);
  }

  public RenjinVersionId getRenjinVersionId() {
    String[] parts = id.split(":");
    return new RenjinVersionId(parts[3]);
  }
  

  public String getPackageName() {
    return getPackageVersionId().getPackageName();
  }

  public String getVersion() {
    return getPackageVersionId().getVersionString();
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

  public long getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(long buildNumber) {
    this.buildNumber = buildNumber;
  }

  public Set<String> getBlockingDependencies() {
    return blockingDependencies;
  }

  public Set<PackageVersionId> getBlockingDependencyIds() {
    Set<PackageVersionId> ids = Sets.newHashSet();
    for(String string : blockingDependencies) {
      ids.add(new PackageVersionId(string));
    }
    return ids;
  }

  public void setBuildDelta(Delta delta) {
    this.buildDelta = delta.getCode();
  }

  public void setBuildDelta(int buildDelta) {
    this.buildDelta = buildDelta;
  }

  public Delta getBuildDelta() {
    return Delta.valueOf(buildDelta);
  }

  public void setBlockingDependencies(Set<String> blockingDependencies) {
    this.blockingDependencies = blockingDependencies;
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

  public void setDependenciesFrom(Iterable<PackageBuildId> values) {
    this.dependencies = Sets.newHashSet(Iterables.transform(values, Functions.toStringFunction()));
  }

  public void setBlockingDependenciesFrom(Iterable<PackageVersionId> notBuilt) {
    this.blockingDependencies = Sets.newHashSet(Iterables.transform(notBuilt, Functions.toStringFunction()));
  }

  public String getBuildURL() {
    return "/package/" + getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" + getVersion() +
        "/build/" + getBuildNumber();
  }


  public static Ordering<PackageStatus> orderByPackageVersion() {
    return Ordering.natural().onResultOf(new Function<PackageStatus, Comparable>() {
      @Override
      public Comparable apply(PackageStatus input) {
        return input.getPackageVersionId();
      }
    });
  }

  public static Ordering<PackageStatus> orderByRenjinVersion() {
    return Ordering.natural().onResultOf(new Function<PackageStatus, Comparable>() {
      @Override
      public Comparable apply(PackageStatus input) {
        return new DefaultArtifactVersion(input.getRenjinVersionId().toString());
      }
    });
  }

  public Integer getTestCount() {
    return testCount;
  }

  public void setTestCount(Integer testCount) {
    this.testCount = testCount;
  }

  public Integer getTestPassCount() {
    return testPassCount;
  }

  public void setTestPassCount(Integer testPassCount) {
    this.testPassCount = testPassCount;
  }
}
