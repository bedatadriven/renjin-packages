package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * View model for Test history
 */
public class DepMatrixPage {

  private PackageVersion packageVersion;
  private Optional<String> testName;
  
  private Set<PackageId> dependencies = Sets.newHashSet();
  private List<DepMatrixRow> rows = Lists.newArrayList();
  
  public DepMatrixPage(PackageVersion packageVersion, Optional<String> testName) {
    this.packageVersion = packageVersion;
    this.testName = testName;

    // Include only the test results from the most recent build per Renjin version
    Map<PackageBuildId, PackageTestResult> resultMap = Maps.newHashMap();
    if(testName.isPresent()) {
      Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersion.getPackageId(), testName.get());
      for (PackageTestResult result : results) {
        resultMap.put(result.getBuildId(), result);
      }
    }
    // Build view model for each Build
    Iterable<PackageBuild> builds = PackageDatabase.getBuilds(packageVersion.getPackageVersionId()).iterable();
    for (PackageBuild build : builds) {
      DepMatrixRow row = new DepMatrixRow(build, resultMap.get(build.getId()));
      dependencies.addAll(row.getDependencies());
      rows.add(row);
    }
  }

  public PackageVersion getPackageVersion() {
    return packageVersion;
  }


  
  public Collection<PackageId> getDependencies() {
    return dependencies;
  }

  public List<DepMatrixRow> getRows() {
    return rows;
  }
  
  public boolean isTestPresent() {
    return testName.isPresent();
  }
  
  public String getTestName() {
    return testName.get();
  }
}

