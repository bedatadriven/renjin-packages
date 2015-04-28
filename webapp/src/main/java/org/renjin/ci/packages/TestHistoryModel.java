package org.renjin.ci.packages;

import org.renjin.ci.datastore.PackageTestResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestHistoryModel {
  
  private List<PackageTestResult> results;
  private Set<String> tests;
  private Set<Long> testRuns;

  public TestHistoryModel(List<PackageTestResult> results) {
    this.results = results;
    this.tests = new HashSet<>();
    for (PackageTestResult result : results) {
      tests.add(result.getName());
    }
  }
}
