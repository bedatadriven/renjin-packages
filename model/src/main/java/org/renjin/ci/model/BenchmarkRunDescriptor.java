package org.renjin.ci.model;

import java.util.Map;

/**
 * Describes a new benchmark run
 */
public class BenchmarkRunDescriptor {

  private MachineDescriptor machine;
  
  private Map<String, String> runVariables;

  /**
   * The repo of the benchmarks
   */
  private String repoUrl;

  /**
   * The commit id of the benchmarks at the time of the run
   */
  private String commitId;


  /**
   * The contents of /proc/cpuinfo
   */
  private String cpuInfo;


  /**
   * The interpreter used (Renjin, GNU R, pqR, etc)
   */
  private String interpreter;
  

  /**
   * The version of the interpreter used
   */
  private String interpreterVersion;
  private int harnessVersion;

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


  public MachineDescriptor getMachine() {
    return machine;
  }

  public void setMachine(MachineDescriptor machine) {
    this.machine = machine;
  }

  public Map<String, String> getRunVariables() {
    return runVariables;
  }

  public void setRunVariables(Map<String, String> runVariables) {
    this.runVariables = runVariables;
  }

  public void setHarnessVersion(int harnessVersion) {
    this.harnessVersion = harnessVersion;
  }

  public int getHarnessVersion() {
    return harnessVersion;
  }
}

