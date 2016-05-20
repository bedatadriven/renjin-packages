package org.renjin.ci.packages;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * View model for Test history
 */
public class TestHistoryPage {

  private PackageVersion packageVersion;
  private String testName;
  
  private Multimap<RenjinVersionId, TestHistoryResult> resultMap = HashMultimap.create();
  private Set<PackageId> dependencies = Sets.newHashSet();
  
  public TestHistoryPage(PackageVersion packageVersion, String testName) {
    this.packageVersion = packageVersion;
    this.testName = testName;

    // Include only the test results from the most recent build per Renjin version
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersion.getPackageVersionId(), testName);
    Iterable<PackageBuild> builds = PackageDatabase.getBuilds(packageVersion.getPackageVersionId()).iterable();
    
    // Merge build data with test results
    Map<PackageBuildId, PackageBuild> buildMap = Maps.newHashMap();
    for (PackageBuild build : builds) {
      buildMap.put(build.getId(), build);
    }
    
    // Build view model for each 
    for (PackageTestResult result : results) {
      PackageBuild build = buildMap.get(result.getBuildId());
      TestHistoryResult resultModel = new TestHistoryResult(build, result);
      
      resultMap.put(build.getRenjinVersionId(), resultModel);
      dependencies.addAll(resultModel.getDependencies());
    }
  }

  public PackageVersion getPackageVersion() {
    return packageVersion;
  }

  public String getTestName() {
    return testName;
  }

  public Collection<TestHistoryResult> getResults() {
    return resultMap.values();
  }
  
  
  public Collection<RenjinVersionId> getRenjinVersions() {
    return resultMap.keySet();
  }
  
  public Collection<PackageId> getDependencies() {
    return dependencies;
  }
  
  public Collection<TestHistoryResult> getResults(RenjinVersionId renjinVersion) {
    return resultMap.get(renjinVersion);
  }
}
