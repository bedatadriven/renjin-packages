package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfNull;
import com.googlecode.objectify.condition.IfZero;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * Records metadata about an individual package
 * build attempt
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageBuild {

  @Parent
  private Key<PackageVersion> versionKey;
  
  @Id
  private long buildNumber;

  @JsonProperty
  private BuildOutcome outcome;

  @JsonProperty
  private NativeOutcome nativeOutcome;

  /**
   * The Renjin version against which the
   * package was built
   */
  @JsonProperty
  private String renjinVersion;

  @Unindex
  @JsonProperty
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
  
  @Index
  @IgnoreSave(IfZero.class)
  private byte buildDelta;
  
  
  public PackageBuild() {
  }

  public PackageBuild(PackageVersionId packageVersionId, long buildNumber) {
    this.versionKey = PackageVersion.key(packageVersionId);
    this.buildNumber = buildNumber;
  }

  public static Key<PackageBuild> key(PackageVersionId packageVersionId, long buildNumber) {
    return Key.create(PackageVersion.key(packageVersionId), PackageBuild.class, buildNumber);
  }

  public String getLogPath() {
    return getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" +
        getPackageVersionId().getVersionString() + "-b" + buildNumber + ".log";
  }
  
  public static String getLogUrl(PackageBuildId id) {
    return "http://storage.googleapis.com/renjinci-logs/" + id.getGroupId() + "/" + id.getPackageName() + "/" +
        id.getPackageVersionId().getVersionString() + "-b" + id.getBuildNumber() + ".log";
  }
  

  public PackageBuildId getId() {
    return new PackageBuildId(getPackageVersionId(), buildNumber);
  }


  public String getPackageName() {
    return getPackageVersionId().getPackageName();
  }

  @JsonIgnore
  public String getPath() {
    return getId().toString().replaceAll(":", "/");
  }


  public String getVersion() {
    return getPackageVersionId().getVersionString();
  }

  public PackageId getPackageId() {
    return PackageId.valueOf(versionKey.getParent().getName());
  }
  
  public PackageVersionId getPackageVersionId() {
    return new PackageVersionId(getPackageId(), versionKey.getName());
  }

  public long getBuildNumber() {
    return buildNumber;
  }

  public String getBuildVersion() {
    return getVersion() + "-b" + getBuildNumber();
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

  @JsonSetter
  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public void setRenjinVersion(RenjinVersionId release) {
    this.renjinVersion = release.toString();
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  @JsonSetter
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

  public byte getBuildDelta() {
    return buildDelta;
  }

  public void setBuildDelta(byte buildDelta) {
    this.buildDelta = buildDelta;
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
    return "/package/" + getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" + getVersion() +
        "/build/" + getBuildNumber();
  }

  public String getLogUrl() {
    return "//storage.googleapis.com/renjinci-logs/" + getLogPath();
  }
  

  public static Ordering<PackageBuild> orderByNumber() {
    return Ordering.natural().onResultOf(new Function<PackageBuild, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(PackageBuild input) {
        return input.getBuildNumber();
      }
    });
  }

  /**
   *
   * @return true if the build completed (was successful, failure, error, or timeout), but
   * not null or cancelled
   */
  public boolean isFinished() {
    if(outcome == null) {
      return false;
    }

    switch (outcome) {
      case SUCCESS:
      case FAILURE:
      case ERROR:
      case TIMEOUT:
        return true;

      default:
      case CANCELLED:
        return false;
    }
  }

  public boolean isFailed() {
    if(outcome == null) {
      return false;
    }
    switch (outcome) {
      case ERROR:
      case FAILURE:
      case TIMEOUT:
        return true;

      default:
        return false;
    }
  }
}
