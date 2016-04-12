package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.RenjinVersionId;

import java.util.List;
import java.util.TreeMap;

/**
 * View model for Test history
 */
public class TestHistoryPage {

  private PackageVersion packageVersion;
  private String testName;
  private List<PackageTestResult> results = Lists.newArrayList();

  public TestHistoryPage(PackageVersion packageVersion, String testName) {
    this.packageVersion = packageVersion;
    this.testName = testName;

    // Include only the test results from the most recent build per Renjin version
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersion.getPackageVersionId(), testName);

    TreeMap<RenjinVersionId, PackageTestResult> map = Maps.newTreeMap();
    for (PackageTestResult result : results) {
      RenjinVersionId rv = result.getRenjinVersionId();
      if(!map.containsKey(rv)) {
        map.put(rv, result);
      } else {
        if(map.get(rv).getPackageBuildNumber() < result.getPackageBuildNumber()) {
          map.put(rv, result);
        }
      }
    }
      
    this.results = Lists.newArrayList(map.values());
  }

  public PackageVersion getPackageVersion() {
    return packageVersion;
  }

  public String getTestName() {
    return testName;
  }

  public List<PackageTestResult> getResults() {
    return results;
  }
}
