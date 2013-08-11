package org.renjin.cran;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.renjin.cran.BuildReport.PackageReport;

public class BuildStatistics {

  private int totalPackages;
  private int totalPackagesBuilt;
  private int totalTests;
  private int totalTestsPassed;
  
  public BuildStatistics(Collection<PackageReport> reports) {
    for(PackageReport report : reports) {
      try {
        sumPackage(report);
        sumTests(report);
        
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void sumPackage(PackageReport report) {
    totalPackages ++;
    if(report.getOutcome().equals("success")) {
      totalPackagesBuilt++;
    }
  }

  private void sumTests(PackageReport report) throws IOException {
    List<TestResultParser> testResults = report.getTestResults();
    totalTests += testResults.size();
    for(TestResultParser test : testResults) {
      if(test.isPassed()) {
        totalTestsPassed++;
      }
    }
  }

  public int getTotalPackages() {
    return totalPackages;
  }

  public int getTotalPackagesBuilt() {
    return totalPackagesBuilt;
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getTotalTestsPassed() {
    return totalTestsPassed;
  }
  
}
