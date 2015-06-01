package org.renjin.ci.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the results of a set of tests 
 */
public class TestResultSet {


  private final List<TestResult> results = new ArrayList<>();

  public List<TestResult> getResults() {
    return results;
  }

}
