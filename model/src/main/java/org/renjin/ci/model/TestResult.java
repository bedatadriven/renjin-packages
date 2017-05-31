package org.renjin.ci.model;

/**
 * The result of a test run
 */
public class TestResult {
  private String name;
  private boolean passed;
  private long duration;
  private boolean output;
  private String failureMessage;
  private TestType testType;


  public String getName() {
    return name;
  }

  public void setName(String id) {
    this.name = id;
  }

  public void setOutput(boolean output) {
    this.output = output;
  }

  public boolean isOutput() {
    return output;
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

  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public TestType getTestType() {
    return testType;
  }

  public void setTestType(TestType testType) {
    this.testType = testType;
  }
}
