package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.*;


public class PackageBuildPage {
  private final List<PackageTestResult> testResults;
  private PackageBuildId buildId;
  private PackageBuild build;
  
  private LoadResult<PackageVersionDelta> deltas;
  
  private List<RenjinBuildHistory> histories = new ArrayList<>();

  private final LoadResult<RenjinRelease> renjinVersion;
  private final LoadResult<RenjinRelease> previousRenjinVersion;

  private List<Test> tests;
  
  public PackageBuildPage(PackageBuildId buildId) {
    this.buildId = buildId;
    
    // Start queries running in background
    Iterable<PackageBuild> buildQuery = PackageDatabase.getBuilds(buildId.getPackageVersionId()).iterable();
    Iterable<PackageTestResult> testsQuery = PackageDatabase.getTestResults(buildId);
    deltas = PackageDatabase.getDelta(buildId.getPackageVersionId());
    
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


    // Fetch this Renjin release and the previous
    this.renjinVersion = PackageDatabase.getRenjinRelease(build.getRenjinVersionId());
    RenjinVersionId previousRenjinVersion = buildMap.keySet().lower(build.getRenjinVersionId());
    if(previousRenjinVersion != null) {
      this.previousRenjinVersion = PackageDatabase.getRenjinRelease(previousRenjinVersion);
    } else {
      this.previousRenjinVersion = null;
    }
  }
  
  public List<String> getUpstreamBuilds() {
    return build.getResolvedDependencies();
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
  
  public String getLogUrl() {
    return StorageKeys.buildLogUrl(buildId);
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
 
  public List<Test> getTestResults() {
    
    if(tests == null) {
      
      tests = new ArrayList<>();
      
      Set<String> regressions = Collections.emptySet();
      Set<String> progressions = Collections.emptySet();

      if (deltas.now() != null) {
        Optional<BuildDelta> delta = deltas.now().getBuild(buildId);
        if (delta.isPresent()) {
          regressions = delta.get().getTestRegressions();
          progressions = delta.get().getTestProgressions();
        }
      }

      for (PackageTestResult testResult : testResults) {
        Test test = new Test(testResult);
        test.regression = regressions.contains(testResult.getName());
        tests.add(test);
      }
    }
    return tests;
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
  
  public String getPreviousBuildRenjinVersion() {
    if(previousRenjinVersion == null) {
      return null;
    }

    RenjinRelease version = previousRenjinVersion.now();
    if(version == null) {
      // missing record?
      return null;
    }
    return version.getVersion();
  }
  
  
  public String getGitHubCompareUrl() {
    if(previousRenjinVersion == null) {
      return null;
    }

    RenjinRelease currentRelease = this.renjinVersion.now();
    if(currentRelease == null) {
      return null;
    }
    RenjinRelease previousRelease = previousRenjinVersion.now();
    if(previousRelease == null) {
      return null;
    }
    
    String currentCommit = currentRelease.getCommitSha1();
    String previousCommit = previousRelease.getCommitSha1();
    
    return "https://github.com/bedatadriven/renjin/compare/" + previousCommit + "..." + currentCommit;
  }
  
  public class Test {
    
    private PackageTestResult result;
    private boolean regression;

    public Test(PackageTestResult testResult) {
      this.result = testResult;
    }
    
    public String getName() {
      return result.getName();
    }

    public long getDuration() {
      return result.getDuration();
    }
    
    public boolean isPassed() {
      return result.isPassed();
    }

    public boolean isRegression() {
      return regression;
    }
    
    public String getLogUrl() {
      return result.getLogUrl();
    }

    public boolean isOutput() {
      return result.isOutput();
    }

    public String getFailureMessage() {
      return result.getFailureMessage();
    }
  }
}
