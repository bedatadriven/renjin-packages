package org.renjin.ci.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;

import java.util.Date;
import java.util.Set;

/**
 * Records metadata about an individual package
 * build attempt
 */
@Entity
public class PackageBuild {

  @Id
  private String id;

  private BuildOutcome outcome;

  private NativeOutcome nativeOutcome;

  /**
   * The Renjin version against which the
   * package was built
   */
  private String renjinVersion;

  @Unindex
  private Set<String> dependencies;

  @IgnoreSave(IfNull.class)
  private String pom;

  @Index
  @IgnoreSave(IfNull.class)
  private Long startTime;

  @Index
  @IgnoreSave(IfNull.class)
  private Long endTime;

  @IgnoreSave(IfNull.class)
  private String workerId;

  @Unindex
  @IgnoreSave(IfNull.class)
  private Long duration;

  public PackageBuild() {
  }

  public PackageBuild(PackageVersionId packageVersionId, long buildNumber) {
    this.id = keyName(packageVersionId, buildNumber);
  }

  public static Key<PackageBuild> key(PackageVersionId packageVersionId, long buildNumber) {
    return Key.create(PackageBuild.class, keyName(packageVersionId, buildNumber));
  }

  private static String keyName(PackageVersionId packageVersionId, long buildNumber) {
    return packageVersionId.toString() + ":" + buildNumber;
  }

  public String getPath() {
    return id.replaceAll(":", "/");
  }

  public String getLogPath() {
    return getBuildNumber() + "/" + getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" +
        getPackageVersionId().getVersionString() + ".log";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  public String getPackageName() {
    String[] parts = id.split(":");
    return parts[1];
  }

  public String getVersion() {
    return getPackageVersionId().getVersionString();
  }

  public PackageVersionId getPackageVersionId() {
    String[] parts = id.split(":");
    return new PackageVersionId(parts[0], parts[1], parts[2]);
  }

  public long getBuildNumber() {
    String[] parts = id.split(":");
    return Long.parseLong(parts[3]);
  }

  public String getGroupId() {
    return getPackageVersionId().getGroupId();
  }

  public boolean isSucceeded() {
    return outcome == BuildOutcome.SUCCESS;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public RenjinVersionId getRenjinVersionId() {
    return new RenjinVersionId(renjinVersion);
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public void setRenjinVersion(RenjinVersionId release) {
    this.renjinVersion = release.toString();
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

  public String getPom() {
    return pom;
  }

  public void setPom(String pom) {
    this.pom = pom;
  }

  /**
   *
   * @return the start time of a build in progress, or null if the build is complete
   */
  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public NativeOutcome getNativeOutcome() {
    return nativeOutcome;
  }

  public void setNativeOutcome(NativeOutcome nativeOutcome) {
    this.nativeOutcome = nativeOutcome;
  }

  /**
   *
   * @return the start time of a build in progress, or null if the build is complete
   */
  public Date getStartDate() {
    if(startTime != null) {
      return new Date(startTime);
    } else {
      return null;
    }
  }

  public Date getEndDate() {
    if(endTime == null) {
      return null;
    }
    return new Date(endTime);
  }

  public String getWorkerId() {
    return workerId;
  }

  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  public String getResultURL() {
    return "/build/result/" + getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" + getVersion() +
        "/" + getBuildNumber();
  }
}
