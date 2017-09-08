package org.renjin.ci.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.PullBuildId;
import org.renjin.ci.storage.StorageKeys;

/**
 * Record of a package build against a Pull Request build.
 */
@Entity
public class PullPackageBuild {


  @Parent
  private Key<PullBuild> pullBuild;

  @Id
  private String packageVersionId;

  @JsonProperty
  private BuildOutcome outcome;


  @JsonProperty
  private NativeOutcome nativeOutcome;


  @Unindex
  private long timestamp;

  /**
   * The ID of the release build to which this build was compared.
   */
  @Unindex
  private PackageBuild releaseBuild;

  @Unindex
  private int testRegressionCount;

  @Unindex
  private int testProgressionCount;


  public static Key<PullPackageBuild> key(PullBuildId pullBuildId, PackageVersionId packageVersionId) {
    return Key.create(PullBuild.key(pullBuildId), PullPackageBuild.class, packageVersionId.toString());
  }

  public PullPackageBuild() {
  }

  public PullPackageBuild(long pullNumber, long pullBuildNumber, PackageVersionId packageVersionId) {
    this.pullBuild = PullBuild.key(pullNumber, pullBuildNumber);
    this.packageVersionId = packageVersionId.toString();
  }

  public PullBuildId getPullBuildId() {
    return PullBuild.idFromKey(pullBuild);
  }

  public PackageVersionId getPackageVersionId() {
    return PackageVersionId.fromTriplet(packageVersionId);
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  public NativeOutcome getNativeOutcome() {
    return nativeOutcome;
  }

  public void setNativeOutcome(NativeOutcome nativeOutcome) {
    this.nativeOutcome = nativeOutcome;
  }

  public String getLogUrl() {
    return StorageKeys.buildLogUrl(getPackageVersionId(), getPullBuildId().getBuildNumber()) +
        "?timestamp=" + timestamp;
  }


  public String getPath() {
    return "/pull/" + getPullBuildId().getPullNumber() + "/build/" + getPullBuildId().getPullBuildNumber() +
          "/package/" + getPackageVersionId().getGroupId() + "/" + getPackageVersionId().getPackageName() +
          "/" + getPackageVersionId().getVersionString();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public PackageBuild getReleaseBuild() {
    return releaseBuild;
  }

  public void setReleaseBuild(PackageBuild releaseBuild) {
    this.releaseBuild = releaseBuild;
  }

  public int getTestRegressionCount() {
    return testRegressionCount;
  }

  public void setTestRegressionCount(int testRegressionCount) {
    this.testRegressionCount = testRegressionCount;
  }

  public int getTestProgressionCount() {
    return testProgressionCount;
  }

  public void setTestProgressionCount(int testProgressionCount) {
    this.testProgressionCount = testProgressionCount;
  }

  public boolean isNativeRegression() {
    return nativeOutcome == NativeOutcome.FAILURE &&
        releaseBuild != null &&
        releaseBuild.getNativeOutcome() == NativeOutcome.SUCCESS;
  }

  public boolean isNativeProgression() {
    return nativeOutcome == NativeOutcome.SUCCESS &&
        releaseBuild != null &&
        releaseBuild.getNativeOutcome() == NativeOutcome.FAILURE;
  }


  public String getJenkinsBuildPath() {
    return String.format("http://build.renjin.org/job/Regression-Testing/job/Rebuild-Package/parambuild?PR=%d&PR_BUILD=%d&PACKAGE_VERSION_ID=%s",
        getPullBuildId().getPullNumber(),
        getPullBuildId().getPullBuildNumber(),
        getPackageVersionId().toString());
  }

}
