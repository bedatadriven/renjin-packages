package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import com.googlecode.objectify.condition.IfTrue;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.PullBuildId;
import org.renjin.ci.model.TestType;
import org.renjin.ci.storage.StorageKeys;

@Entity
public class PullTestResult {

  @Parent
  private Key<PullPackageBuild> packageBuild;

  @Id
  private String name;

  @Unindex
  private long timestamp;

  public PullTestResult() {
  }

  public PullTestResult(Key<PullPackageBuild> packageBuild, String name) {
    this.packageBuild = packageBuild;
    this.name = name;
  }

  @Index
  private boolean passed;

  @Unindex
  private Long duration;

  @Unindex
  @IgnoreSave(IfNull.class)
  private String error;

  @Unindex
  @IgnoreSave(IfTrue.class)
  private boolean output = true;

  @Index
  private TestType testType = TestType.OTHER;

  @Unindex
  @IgnoreSave(IfNull.class)
  private String failureMessage;

  private PackageTestResult releaseResult;


  public String getName() {
    return name;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public boolean isOutput() {
    return output;
  }

  public void setOutput(boolean output) {
    this.output = output;
  }

  public TestType getTestType() {
    return testType;
  }

  public void setTestType(TestType testType) {
    this.testType = testType;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
  }


  public String getLogUrl() {
    return StorageKeys.testLogUrl(getPackageVersionId(), getBuildId().getBuildNumber(), name) +
        "?timestamp=" + timestamp;
  }

  private PullBuildId getBuildId() {
    Key<PullBuild> grandParentKey = packageBuild.getParent();
    return PullBuild.idFromKey(grandParentKey);
  }

  private PackageVersionId getPackageVersionId() {
    return PackageVersionId.fromTriplet(packageBuild.getName());
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public PackageTestResult getReleaseResult() {
    return releaseResult;
  }

  public void setReleaseResult(PackageTestResult releaseResult) {
    this.releaseResult = releaseResult;
  }

  public boolean isRegression() {
    if(releaseResult != null) {
      if(releaseResult.isPassed() && !isPassed()) {
        return true;
      }
    }
    return false;
  }

  public boolean isProgression() {
    if(releaseResult != null) {
      if(isPassed() && !releaseResult.isPassed()) {
        return true;
      }
    }
    return false;
  }
}
