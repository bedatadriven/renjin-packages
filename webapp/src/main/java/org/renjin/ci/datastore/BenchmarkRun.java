package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Date;

/**
 * Describes a single run of a set of benchmarks
 */
@Entity
public class BenchmarkRun {
  
  @Id
  private Long id;

  /**
   * The time the run was started
   */
  private Date startTime;

  /**
   * The repo of the benchmarks
   */
  private String repoUrl;

  /**
   * The commit id of the benchmarks at the time of the run
   */
  @Unindex
  private String commitId;


  /**
   * The id of the machine
   */
  @Index
  private String machineId;
  
  
  /**
   * The interpreter used (Renjin, GNU R, pqR, etc)
   */
  private String interpreter;


  /**
   * The version of the interpreter used
   */
  private String interpreterVersion;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

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
