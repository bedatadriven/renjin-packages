package org.renjin.repo.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

public class BenchmarkResult {


  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @ManyToOne
  private Benchmark benchmark;

  @ManyToOne
  private Hardware hardware;

  private String interpreter;

  private String interpreterVersion;

  private String interpreterCommitId;

  private int iterations;

  private long elapsedTime;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Benchmark getBenchmark() {
    return benchmark;
  }

  public void setBenchmark(Benchmark benchmark) {
    this.benchmark = benchmark;
  }

  public Hardware getHardware() {
    return hardware;
  }

  public void setHardware(Hardware hardware) {
    this.hardware = hardware;
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

  public String getInterpreterCommitId() {
    return interpreterCommitId;
  }

  public void setInterpreterCommitId(String interpreterCommitId) {
    this.interpreterCommitId = interpreterCommitId;
  }

  public int getIterations() {
    return iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }
}
