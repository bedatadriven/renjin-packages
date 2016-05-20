package org.renjin.ci.packages;

import com.google.common.collect.Maps;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collection;
import java.util.Map;

public class TestHistoryResult {
  private final PackageBuild build;
  private final PackageTestResult result;
  private final Map<PackageId, PackageVersionId> dependencyVersion = Maps.newHashMap();

  public TestHistoryResult(PackageBuild build, PackageTestResult result) {
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
  
  public boolean isPassed() {
    return result.isPassed();
  }
  
  public long getDuration() {
    return result.getDuration();
  }
  
  public String getLogUrl() {
    return result.getLogUrl();
  }
  
  public String getDependencyVersion(PackageId packageId) {
    PackageVersionId packageVersionId = dependencyVersion.get(packageId);
    if(packageVersionId == null) {
      return "?";
    } 
    return packageVersionId.getVersionString();
  }
  
  public PackageBuildId getBuildId() {
    return result.getBuildId();
  }
  
  public RenjinVersionId getRenjinVersion() {
    return result.getRenjinVersionId();
  }
  
  public String getMarkUrl() {
    return result.getPackageVersionId().getPath() + "/test/" + result.getName() + "/mark";
  }
  
  public String getStatus() {
    if(result.isManualFail()) {
      return "MARKED AS FAILED";
    } else if(result.isPassed()) {
      return "PASSED";
    } else {
      return "FAILED";
    }
  } 
}
