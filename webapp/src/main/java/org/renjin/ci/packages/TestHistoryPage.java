package org.renjin.ci.packages;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collection;

/**
 * View model for test history page
 */
public class TestHistoryPage {


  private final PackageVersion packageVersion;
  private final String testName;

  private final Multimap<RenjinVersionId, PackageTestResult> resultMap = HashMultimap.create();

  private final boolean reliable;
  
  public TestHistoryPage(PackageVersion packageVersion, String testName, Long pullNumber) {
    this.packageVersion = packageVersion;
    this.testName = testName;

    // Include only the test results from the most recent build per Renjin version
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersion.getPackageVersionId(), testName);
    for (PackageTestResult result : results) {
      resultMap.put(result.getRenjinVersionId(), result);
    }
    
    reliable = DeltaBuilder.reliableTest(results);

    if(pullNumber != null) {

    }
  }

  public String getTestName() {
    return testName;
  }

  public PackageVersion getPackageVersion() {
    return packageVersion;
  }
  
  public Collection<RenjinVersionId> getRenjinVersions() {
    return resultMap.keySet();
  }
  
  public Collection<PackageTestResult> getResults(RenjinVersionId renjinVersionId) {
    return resultMap.get(renjinVersionId);
  }
  
  public Collection<PackageTestResult> getResults() {
    return resultMap.values();
  }

  public boolean isReliable() {
    return reliable;
  }
}
