package org.renjin.ci.packages;

import com.google.common.collect.Maps;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.util.Collection;
import java.util.Map;

public class DepMatrixRow {
  private final PackageBuild build;
  private final PackageTestResult result;
  private final Map<PackageId, PackageVersionId> dependencyVersion = Maps.newHashMap();

  public DepMatrixRow(PackageBuild build, PackageTestResult result) {
    this.build = build;
    this.result = result;

    for (PackageVersionId packageVersionId : build.getResolvedDependencyIds()) {
      dependencyVersion.put(packageVersionId.getPackageId(), packageVersionId);
    }
  }
  
  public Collection<PackageId> getDependencies() {
    return dependencyVersion.keySet();
  }
  
  public long getBuildNumber() {
    return build.getBuildNumber();
  }
  
  public String getDependencyVersion(PackageId packageId) {
    PackageVersionId packageVersionId = dependencyVersion.get(packageId);
    if(packageVersionId == null) {
      return "?";
    } 
    return packageVersionId.getVersionString();
  }

  public PackageBuild getBuild() {
    return build;
  }
  
  public boolean isCompilationAttempted() {
    return build.getNativeOutcome() != null && build.getNativeOutcome() != NativeOutcome.NA;
  }
  
  public boolean isCompilationSuccessful() {
    return build.getNativeOutcome() == NativeOutcome.SUCCESS;
  }
  
  public boolean isTestRun() {
    return result != null;
  }
  
  public String getTestResult() {
    if(result.isPassed()) {
      return "PASSED";
    } else if(result.isManualFail()) {
      return "MARKED AS FAILED";
    } else {
      return "FAILED";
    }
  }
}
