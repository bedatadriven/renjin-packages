package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import org.renjin.ci.archive.BuildLogs;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class PackageBuildPage {
  private final List<PackageTestResult> testResults;
  private PackageBuildId buildId;
  private final String logText;
  private PackageBuild build;
  
  private List<RenjinBuildHistory> histories = new ArrayList<>();

  public PackageBuildPage(PackageBuildId buildId) {
    this.buildId = buildId;
    
    // Start queries running in background
    Iterable<PackageBuild> buildQuery = PackageDatabase.getBuilds(buildId.getPackageVersionId()).iterable();
    Iterable<PackageTestResult> testsQuery = PackageDatabase.getTestResults(buildId);
    logText = BuildLogs.tryFetchLog(buildId);
    
    // Aggregate query results together
    this.testResults = Lists.newArrayList(testsQuery);
    this.build = findBuild(buildQuery);
    
    // build a map to the latest 
    TreeMultimap<RenjinVersionId, PackageBuild> buildMap = TreeMultimap.create(
        Ordering.natural(),
        PackageBuild.orderByNumber().reverse());
    
    for (PackageBuild build : buildQuery) {
      if(build.getOutcome() != null) {
        buildMap.put(build.getRenjinVersionId(), build);
      }
    }

    for (RenjinVersionId renjinVersion : buildMap.keySet()) {
      histories.add(new RenjinBuildHistory(renjinVersion, buildMap.get(renjinVersion)));
    }
  }
  
  public long getBuildNumber() {
    return buildId.getBuildNumber();
  }
  
  public PackageVersionId getVersionId() {
    return build.getPackageVersionId();
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
  
  private PackageBuild findBuild(Iterable<PackageBuild> buildQuery) {
    for (PackageBuild build : buildQuery) {
      if (build.getBuildNumber() == buildId.getBuildNumber()) {
        return build;
      }
    }
    throw new WebApplicationException(Response.Status.NOT_FOUND);
  }

  public String getLogText() {
    return logText;
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

  public List<RenjinBuildHistory> getRenjinHistory() {
    return histories;
  }
}
