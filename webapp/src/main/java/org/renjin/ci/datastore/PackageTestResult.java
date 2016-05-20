package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.storage.StorageKeys;

@Entity
public class PackageTestResult {
  
  @Parent
  private Key<PackageBuild> buildKey;


  /**
   * The name of the test
   */
  @Id
  private String name;

  @Unindex
  private String renjinVersion;
  
  @Index
  private boolean passed;
  
  @Unindex
  private Long duration;
  
  @Unindex
  @IgnoreSave(IfNull.class)
  private String error;

  
  @Index
  @IgnoreSave(IfFalse.class)
  private boolean manualFail;
  
  @Unindex
  private String manualFailReason;
  
  
  public PackageTestResult() {
  }

  public static Key<PackageTestResult> key(PackageBuildId buildId, String testName) {
    return Key.create(PackageBuild.key(buildId), PackageTestResult.class, testName);
  }
  
  public PackageTestResult(Key<PackageBuild> buildKey, String name) {
    this.buildKey = buildKey;
    this.name = name;
  }

  public Key<PackageBuild> getPackageBuildKey() {
    return buildKey;
  }

  public PackageVersionId getPackageVersionId() {
    return getBuildId().getPackageVersionId();
  }

  public PackageBuildId getBuildId() {
    return PackageBuild.idOf(buildKey);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getPackageBuildNumber() {
    return getBuildId().getBuildNumber();
  }

  public RenjinVersionId getRenjinVersionId() {
    return new RenjinVersionId(renjinVersion);
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }
  
  public String getOutput() {
    return "";
  }


  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Long getDuration() {
    return duration;
  }
  
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public String getLogUrl() {
    return "https://storage.googleapis.com/" + StorageKeys.BUILD_LOG_BUCKET + "/" +
        StorageKeys.testLog(getPackageVersionId(), getPackageBuildNumber(), getName());
  }

  /**
   * @return true if this test result has been manually marked as a failure.
   */
  public boolean isManualFail() {
    return manualFail;
  }

  public void setManualFail(boolean manualFail) {
    this.manualFail = manualFail;
  }

  public String getManualFailReason() {
    return manualFailReason;
  }

  public void setManualFailReason(String manualFailReason) {
    this.manualFailReason = manualFailReason;
  }
}
