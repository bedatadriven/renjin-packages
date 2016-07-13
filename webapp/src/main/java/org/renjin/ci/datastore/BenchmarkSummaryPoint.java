package org.renjin.ci.datastore;

public class BenchmarkSummaryPoint {
  
  private String interpreterVersion;
  private long meanRunTime;
  private long runTimeVariance;
  private long runCount;

  public String getInterpreterVersion() {
    return interpreterVersion;
  }

  public void setInterpreterVersion(String interpreterVersion) {
    this.interpreterVersion = interpreterVersion;
  }

  public long getMeanRunTime() {
    return meanRunTime;
  }

  public void setMeanRunTime(long meanRunTime) {
    this.meanRunTime = meanRunTime;
  }

  public long getRunTimeVariance() {
    return runTimeVariance;
  }

  public void setRunTimeVariance(long runTimeVariance) {
    this.runTimeVariance = runTimeVariance;
  }

  public long getRunCount() {
    return runCount;
  }

  public void setRunCount(long runCount) {
    this.runCount = runCount;
  }

  @Override
  public String toString() {
    return "BenchmarkSummaryPoint{" +
        "runCount=" + runCount +
        ", runTimeVariance=" + runTimeVariance +
        ", meanRunTime=" + meanRunTime +
        ", interpreterVersion='" + interpreterVersion + '\'' +
        '}';
  }
}
