package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

@Entity
public class PackageTestResult {
  
  @Parent
  private Key parentKey;


  /**
   * Composite key consisting of testRunNumber:testName
   */
  @Id
  private String name;

  @Unindex
  private String renjinVersion;
  
  @Unindex
  private long packageBuildNumber;
  
  @Index
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
    Key versionKey = PackageVersion.key(packageVersionId).getRaw();
    
    this.parentKey = KeyFactory.createKey(versionKey, "TestRun", testRunNumber);
    this.name = testName;
  }

  public Key getParentKey() {
    return parentKey;
  }

  public void setParentKey(Key parentKey) {
    this.parentKey = parentKey;
  }
  
  public PackageVersionId getPackageVersionId() {
    Key versionKey = parentKey.getParent();
    Key packageKey = versionKey.getParent();
    PackageId packageId = PackageId.valueOf(packageKey.getName());
    return new PackageVersionId(packageId, versionKey.getName());
  }

  public long getTestRunNumber() {
    return parentKey.getId();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getPackageBuildNumber() {
    return packageBuildNumber;
  }

  public void setPackageBuildNumber(long packageBuildNumber) {
    this.packageBuildNumber = packageBuildNumber;
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


}
