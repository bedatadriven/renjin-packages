package org.renjin.ci.pulls;

import com.google.api.client.util.Lists;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PullPackageBuild;
import org.renjin.ci.datastore.PullTestResult;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.PullBuildId;

import java.util.ArrayList;
import java.util.List;

public class PullPackageBuildPage {

  private PullBuildId pullBuildId;
  private PackageVersionId packageVersionId;
  private final ArrayList<PullTestResult> testResults;
  private final PullPackageBuild build;

  public PullPackageBuildPage(PullBuildId pullBuildId, PackageVersionId packageVersionId) {
    this.pullBuildId = pullBuildId;
    this.packageVersionId = packageVersionId;

    build = PackageDatabase.getPullPackageBuild(pullBuildId, packageVersionId).now();
    testResults = Lists.newArrayList(PackageDatabase.getTestResults(pullBuildId, packageVersionId));
  }

  public long getPullNumber() {
    return pullBuildId.getPullNumber();
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public long getPullBuildNumber() {
    return pullBuildId.getPullBuildNumber();
  }

  public String getPackageName() {
    return packageVersionId.getPackageName();
  }

  public BuildOutcome getOutcome() {
    return build.getOutcome();
  }

  public String getPackageVersion() {
    return packageVersionId.getVersionString();
  }

  public PullPackageBuild getBuild() {
    return build;
  }

  public List<PullTestResult> getTestResults() {
    return testResults;
  }

}
