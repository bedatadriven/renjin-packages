package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;


@Entity
public class BenchmarkResult {

  @Id
  private Long id;

  @Index
  private long runId;
  
  @Index
  private String machineId;

  /**
   * The running time of the benchmark, in milliseconds
   */
  @Unindex
  @IgnoreSave(IfNull.class)
  private Long runTime;

  @Index
  private String benchmarkName;
  
  @Unindex
  private String interpreter;
  
  @Unindex
  private String interpreterVersion;

  /**
   * Whether the benchmark successfully completed or not
   */
  private boolean completed;


  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getRunId() {
    return runId;
  }

  public String getMachineId() {
    return machineId;
  }

  public void setMachineId(String machineId) {
    this.machineId = machineId;
  }

  public void setRunId(long runId) {
    this.runId = runId;
  }

  public Long getRunTime() {
    return runTime;
  }

  public void setRunTime(Long runTime) {
    this.runTime = runTime;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public void setBenchmarkName(String benchmarkName) {
    this.benchmarkName = benchmarkName;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
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
