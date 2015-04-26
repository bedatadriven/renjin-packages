package org.renjin.ci.model;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;

@Entity
public class PackageTestResult {
  
  @Parent
  private Key versionKey;


  /**
   * Composite key consisting of testRunNumber:testName
   */
  @Id
  private String id;

  @Unindex
  private String renjinVersion;
  
  @Unindex
  private boolean passed;
  
  @Unindex
  @IgnoreSave(IfNull.class)
  private String output;
  
  @Unindex
  @IgnoreSave(IfNull.class)
  private String error;

  public PackageTestResult() {
  }

  public PackageTestResult(PackageVersionId packageVersionId, long testRunNumber, String testName) {
    this.versionKey = parentKey(packageVersionId);
    this.id = testRunNumber + ":" + testName;
  }

  public static Key parentKey(PackageVersionId packageVersionId) {
    Key packageKey = KeyFactory.createKey("Package", packageVersionId.getPackageId().toString());
    return KeyFactory.createKey(packageKey, "PackageVersion", packageVersionId.getVersionString());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Key getVersionKey() {
    return versionKey;
  }

  public void setVersionKey(Key versionKey) {
    this.versionKey = versionKey;
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
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public long getTestRunNumber() {
    String parts[] = id.split(":");
    return Long.parseLong(parts[0]);
  }

}
