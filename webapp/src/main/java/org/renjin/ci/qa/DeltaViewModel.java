package org.renjin.ci.qa;

import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;


public class DeltaViewModel {
  private PackageVersionId packageVersionId;
  private BuildDelta build;
  private PackageBuildId buildId;

  public DeltaViewModel(PackageVersionId packageVersionId, BuildDelta build) {
    this.packageVersionId = packageVersionId;
    this.build = build;
    this.buildId = new PackageBuildId(packageVersionId, build.getBuildNumber());
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }
  
  public boolean isBuildRegression() {
    return build.getBuildDelta() < 0;
  }
  
  public boolean isBuildProgression() {
    return build.getBuildDelta() > 0;
  }
  
  public boolean isCompilationRegression() {
    return build.getCompilationDelta() < 0;
  }
  
  public boolean isCompilationProgression() {
    return build.getCompilationDelta() > 0;
  }
  
  public int getTestRegressionCount() {
    return build.getTestRegressions().size();
  }
  
  public int getTestProgressionCount() {
    return build.getTestProgressions().size();
  }
  
  public String getResultURL() {
    return buildId.getPath();
  }
}
