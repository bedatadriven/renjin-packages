package org.renjin.ci.model;

/**
 * The result of a test run
 */
public class TestResult {
  private String output;
  private boolean passed;
  private long duration;
  private String renjinVersion;
  private String packageBuildVersion;

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
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

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public String getPackageBuildVersion() {
    return packageBuildVersion;
  }

  public void setPackageBuildVersion(String packageBuildVersion) {
    this.packageBuildVersion = packageBuildVersion;
  }
}
