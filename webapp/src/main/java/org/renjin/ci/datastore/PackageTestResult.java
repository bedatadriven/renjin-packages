package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfNull;
import com.googlecode.objectify.condition.IfTrue;
import org.renjin.ci.model.*;
import org.renjin.ci.storage.StorageKeys;

@Entity
public class PackageTestResult {
  
  @Parent
  private Key<PackageBuild> buildKey;


  /**
   * The name of the test
   */
  @Id
  private String id;
  
  @Index
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
  @IgnoreSave(IfTrue.class)
  private boolean output = true;

  @Index
  private TestType testType = TestType.OTHER;

  @Unindex
  @IgnoreSave(IfNull.class)
  private String failureMessage;

  @Unindex
  private String manualFailReason;
  
  
  public PackageTestResult() {
  }

  public static Key<PackageTestResult> key(PackageBuildId buildId, String testName) {
    return Key.create(PackageBuild.key(buildId), PackageTestResult.class, testName);
  }
  
  public PackageTestResult(Key<PackageBuild> buildKey, String name) {
    this.buildKey = buildKey;
    this.id = name;
    this.name = name;
  }

  public Key<PackageTestResult> getKey() {
    return Key.create(buildKey, PackageTestResult.class, name);
  }
  
  public String getWebSafeKey() {
    return KeyFactory.keyToString(getKey().getRaw());
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
    this.id = name;
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

  public String getFailureMessage() {
    return failureMessage;
  }

  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
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
  
  public String getMarkFormPath() {
    return "/qa/markTestResults?packageId=" + getPackageVersionId().getPackageId() + "&testName=" + name;
  }

  /**
   * @return true if this test has output
   */
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

  @Override
  public String toString() {
    return "PackageTestResult{" +
        name + 
        ", renjinVersion='" + renjinVersion + '\'' +
        ", passed=" + passed +
        '}';
  }

  public TestResult toTestResult() {
    TestResult result = new TestResult();
    result.setName(name);
    result.setTestType(getTestType());
    result.setDuration(duration);
    result.setPassed(passed);
    result.setOutput(isOutput());
    result.setFailureMessage(getFailureMessage());
    return result;
  }
}
