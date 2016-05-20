package org.renjin.ci.qa;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.PackageId;

/**
 * View Model for Mark tests page
 */
public class MarkTestsPage {

  private final Iterable<PackageTestResult> results;
  private final PackageId packageId;
  private final String testName;

  public MarkTestsPage(PackageId packageVersion, String testName) {
    this.packageId = packageVersion;
    this.testName = testName;
    results = PackageDatabase.getTestResults(packageId, testName);
  }

  public Iterable<PackageTestResult> getResults() {
    return Lists.newArrayList(results);
  }

  public PackageId getPackageId() {
    return packageId;
  }

  public String getTestName() {
    return testName;
  }
  
}
