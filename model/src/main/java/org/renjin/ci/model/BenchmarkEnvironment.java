package org.renjin.ci.model;


public class BenchmarkEnvironment {

  private long runTime;

  private String operatingSystem;

  private String interpreter;

  private String interpreterVersion;


  public long getRunTime() {
    return runTime;
  }

  public void setRunTime(long runTime) {
    this.runTime = runTime;
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  public String getInterpreter() {
    return interpreter;
  }

  public void setInterpreter(String interpreter) {
    this.interpreter = interpreter;
  }

  public String getInterpreterVersion() {
    return interpreterVersion;
  }

  public void setInterpreterVersion(String interpreterVersion) {
    this.interpreterVersion = interpreterVersion;
  }
}
