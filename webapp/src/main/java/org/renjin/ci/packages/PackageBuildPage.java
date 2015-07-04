package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import org.renjin.ci.archive.BuildLogs;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;


public class PackageBuildPage {
  private final List<PackageBuild> builds;
  private final List<PackageTestResult> testResults;
  private PackageBuildId buildId;
  private final String logText;
  
  private PackageBuild build;

  public PackageBuildPage(PackageBuildId buildId) {
    this.buildId = buildId;
    
    // Start queries running in background
    Iterable<PackageBuild> buildQuery = PackageDatabase.getBuilds(buildId.getPackageVersionId()).iterable();
    Iterable<PackageTestResult> testsQuery = PackageDatabase.getTestResults(buildId);
    logText = BuildLogs.tryFetchLog(buildId);
    
    // Aggregate query results together
    this.builds = Lists.newArrayList(buildQuery);
    this.testResults = Lists.newArrayList(testsQuery);
    this.build = findBuild();
  }
  
  public long getBuildNumber() {
    return buildId.getBuildNumber();
  }
  
  public String getVersion() {
    return build.getVersion();
  }

  public String getPackageName() {
    return buildId.getPackageName();
  }
  
  public String getGroupId() {
    return buildId.getGroupId();
  }
  
  public PackageBuild getBuild() {
    return build;
  }
  
  public PackageVersionId getPackageVersionId() {
    return buildId.getPackageVersionId();
  }
  
  private PackageBuild findBuild() {
    for (PackageBuild build : builds) {
      if (build.getBuildNumber() == buildId.getBuildNumber()) {
        return build;
      }
    }
    throw new WebApplicationException(Response.Status.NOT_FOUND);
  }

  public String getLogText() {
    return logText;
  }
  
  public List<PackageBuild> getAllBuilds() {
    return builds;
  }
  
  public BuildOutcome getOutcome() {
    return build.getOutcome();
  }

  public String getRenjinVersion() {
    return build.getRenjinVersion();
  }
  
  public List<PackageVersionId> getBlockingDependencies() {
    return build.getBlockingDependencyVersions();
  }
  
  public List<PackageTestResult> getTestResults() {
    return testResults;
  }

  public PackageBuildId getBuildId() {
    return buildId;
  }
 
  public Date getStartTime() {
    return getBuild().getStartDate();
  }
  
}
