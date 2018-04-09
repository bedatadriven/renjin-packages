package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Date;

@Entity
public class TestRegression {

  @Parent
  private Key<PackageVersionDelta> parentKey;
  
  @Id
  private String keyName;

  @Unindex
  private String summary;

  /**
   * The Renjin version that broke this test.
   */
  @Index
  private String renjinVersion;

  @Unindex
  private long lastGoodBuildNumber;

  @Unindex
  private String lastGoodRenjinVersion;

  @Index
  private boolean open;

  @Index
  private TestRegressionStatus status = TestRegressionStatus.UNCONFIRMED;

  /**
   * An ordering for unconfirmed test regressions
   */
  @Index
  @IgnoreSave(IfNull.class)
  private String triage;

  @Index
  @IgnoreSave(IfNull.class)
  private Date dateClosed;

  /**
   * The build number that closed this regression.
   */

  @Unindex
  @IgnoreSave(IfNull.class)
  private long closingBuildNumber;

  @Index
  @IgnoreSave(IfNull.class)
  private String closingRenjinVersion;


  public TestRegression() {
  }

  /**
   * TestRegression is uniquely identified by a test in a package version, and the build id it broke.
   */
  public TestRegression(PackageBuildId packageBuildId, String testName) {
    this.parentKey = PackageVersionDelta.key(packageBuildId.getPackageVersionId());
    this.keyName = keyName(testName, packageBuildId);
  }

  public TestRegression(Key<TestRegression> key) {
    this.parentKey = key.getParent();
    this.keyName = key.getName();
  }

  public static String keyName(String testName, PackageBuildId packageBuildId) {
    return testName + ":" + packageBuildId.getBuildNumber();
  }

  public TestRegressionId getId() {
    return idOf(parentKey, keyName);
  }

  public String getTestName() {
    return getId().getTestName();
  }

  public long getBrokenBuildNumber() {
    return getId().getBrokenBuildNumber();
  }

  public String getTriageOrder() {
    return triageOrder(getBrokenBuildId(), getTestName());
  }

  public static String triageOrder(PackageBuildId buildId, String testName) {
    return buildId.getGroupId() + ":" +
        buildId.getPackageName() + ":" +
        testName + ":" +
        buildId.getPackageVersionId().getVersion();
  }

  public static Key<TestRegression> key(PackageBuildId packageBuildId, String testName) {
    return Key.create(PackageVersionDelta.key(packageBuildId.getPackageVersionId()),
          TestRegression.class, keyName(testName, packageBuildId));
  }

  public static TestRegressionId idOf(Key<TestRegression> key) {
    return idOf(key.<PackageVersionDelta>getParent(), key.getName());
  }

  private static TestRegressionId idOf(Key<PackageVersionDelta> parentKey, String keyName) {
    String[] parts = keyName.split(":");
    String testName = parts[0];
    long buildNumber = Long.parseLong(parts[1]);
    return new TestRegressionId(PackageVersionDelta.idOf(parentKey), testName, buildNumber);
  }

  public PackageVersionId getPackageVersionId() {
    return PackageVersionDelta.idOf(parentKey);
  }

  public PackageId getPackageId() {
    return getPackageVersionId().getPackageId();
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


  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public void setTriageIndex(boolean enabled) {
    if(enabled) {
      this.triage = triageOrder(getBrokenBuildId(), getTestName());
    } else {
      this.triage = null;
    }
  }

  public String getTriage() {
    return triage;
  }

  public Date getDateClosed() {
    return dateClosed;
  }

  public void setDateClosed(Date dateClosed) {
    this.dateClosed = dateClosed;
  }

  public long getLastGoodBuildNumber() {
    return lastGoodBuildNumber;
  }

  public void setLastGoodBuildNumber(long lastGoodBuildNumber) {
    this.lastGoodBuildNumber = lastGoodBuildNumber;
  }

  public String getLastGoodRenjinVersion() {
    return lastGoodRenjinVersion;
  }

  public RenjinVersionId getLastGoodRenjinVersionId() {
    return new RenjinVersionId(lastGoodRenjinVersion);
  }

  public void setLastGoodRenjinVersion(String lastGoodRenjinVersion) {
    this.lastGoodRenjinVersion = lastGoodRenjinVersion;
  }

  /**
   * @return a composite key in the form {testName}:{brokenBuildNumber}
   */
  public String getKeyName() {
    return keyName;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public TestRegressionStatus getStatus() {
    return status;
  }

  public void setStatus(TestRegressionStatus status) {
    this.status = status;
  }

  public String getPath() {
    return getId().getPath();
  }

  public PackageBuildId getBrokenBuildId() {
    return new PackageBuildId(getPackageVersionId(), getBrokenBuildNumber());
  }

  public PackageBuildId getLastGoodBuildId() {
    return new PackageBuildId(getPackageVersionId(), getLastGoodBuildNumber());
  }

  public Key<TestRegression> getKey() {
    return key(getBrokenBuildId(), getTestName());
  }

  public void setClosingBuild(PackageBuildId buildId) {
    this.closingBuildNumber = buildId.getBuildNumber();
  }

  public String getClosingRenjinVersion() {
    return closingRenjinVersion;
  }

  public void setClosingRenjinVersion(RenjinVersionId closingRenjinVersion) {
    this.closingRenjinVersion = closingRenjinVersion.toString();
  }
}
