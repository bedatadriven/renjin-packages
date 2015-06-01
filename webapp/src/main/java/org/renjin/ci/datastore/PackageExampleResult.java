package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;

import java.util.Date;


@Entity
public class PackageExampleResult {

  @Parent
  private Key<PackageExample> exampleKey;
  
  @Id
  private long resultId;
  
  @Unindex
  private String renjinVersion;
  
  @Unindex
  private long packageBuildNumber;
  
  @Index
  private Date runTime;
  
  @Unindex
  private long duration;
  
  @Unindex
  private boolean passed;

  public Key<PackageExample> getExampleKey() {
    return exampleKey;
  }

  public void setExampleKey(Key<PackageExample> exampleKey) {
    this.exampleKey = exampleKey;
  }

  public long getResultId() {
    return resultId;
  }

  public void setResultId(long resultId) {
    this.resultId = resultId;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public long getPackageBuildNumber() {
    return packageBuildNumber;
  }

  public void setPackageBuildNumber(long packageBuildNumber) {
    this.packageBuildNumber = packageBuildNumber;
  }

  public Date getRunTime() {
    return runTime;
  }

  public void setRunTime(Date runTime) {
    this.runTime = runTime;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }
}
