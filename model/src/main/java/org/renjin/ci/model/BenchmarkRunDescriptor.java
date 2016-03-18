package org.renjin.ci.model;

/**
 * Describes a new benchmark run
 */
public class BenchmarkRunDescriptor {


  /**
   * The repo of the benchmarks
   */
  private String repoUrl;

  /**
   * The commit id of the benchmarks at the time of the run
   */
  private String commitId;


  /**
   * The id of the machine
   */
  private String machineId;


  /**
   * The contents of /proc/cpuinfo
   */
  private String cpuInfo;


  /**
   * The interpreter used (Renjin, GNU R, pqR, etc)
   */
  private String interpreter;
  
  private String operatingSystem;
  


  /**
   * The version of the interpreter used
   */
  private String interpreterVersion;

  public String getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(String repoUrl) {
    this.repoUrl = repoUrl;
  }

  public String getCommitId() {
    return commitId;
  }

  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }

  public String getMachineId() {
    return machineId;
  }

  public void setMachineId(String machineId) {
    this.machineId = machineId;
  }

  public String getCpuInfo() {
    return cpuInfo;
  }

  public void setCpuInfo(String cpuInfo) {
    this.cpuInfo = cpuInfo;
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


  public String getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }
}

