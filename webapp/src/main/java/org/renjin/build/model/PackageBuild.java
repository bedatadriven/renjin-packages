package org.renjin.build.model;

import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
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

  /**
   * The Renjin version against which the
   * package was built
   */
  private String renjinVersion;

  @Unindex
  private Set<String> dependencies;

  private String pom;

  @Index
  @IgnoreSave(IfNull.class)
  private Long startTime;

  @Index
  @IgnoreSave(IfNull.class)
  private Long endTime;

  private String workerId;

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
        getPackageVersionId().getSourceVersion() + ".log";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  @Transient
  public String getPackageName() {
    String[] parts = id.split(":");
    return parts[1];
  }

  public String getVersion() {
    return getPackageVersionId().getSourceVersion();
  }

  public PackageVersionId getPackageVersionId() {
    String[] parts = id.split(":");
    return new PackageVersionId(parts[0], parts[1], parts[2]);
  }

  public long getBuildNumber() {
    String[] parts = id.split(":");
    return Long.parseLong(parts[3]);
  }

  @Transient
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

  public Date getStartDate() {
    return new Date(startTime);
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
}
