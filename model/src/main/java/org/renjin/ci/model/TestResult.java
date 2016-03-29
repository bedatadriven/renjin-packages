package org.renjin.ci.model;

/**
 * The result of a test run
 */
public class TestResult {
  private String name;
  private boolean passed;
  private long duration;

  public String getName() {
    return name;
  }

  public void setName(String id) {
    this.name = id;
  }
  
  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }
}
