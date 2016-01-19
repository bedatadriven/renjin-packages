package org.renjin.ci.packages.results;

import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.PackageBuildId;

/**
 * Model for a page that helps diagnose a test regression
 */
public class TestRegressionPage {
  


  public static TestRegressionPage query(PackageBuildId buildId, String testName) {
    Iterable<PackageTestResult> testResults = PackageDatabase.getTestResults(buildId.getPackageVersionId(), testName);

    
    
    return null;
  }
}
