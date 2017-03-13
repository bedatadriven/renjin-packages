package org.renjin.ci.datastore;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.*;


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

  @Unindex
  private Map<String, String> runVariables;

  /**
   * Whether the benchmark successfully completed or not
   */
  private boolean completed;
  private int harnessVersion;


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

  public Map<String, String> getRunVariables() {
    return runVariables;
  }

  public void setRunVariables(Map<String, String> runVariables) {
    this.runVariables = runVariables;
  }

  public String getBlas() {
    if(runVariables == null) {
      return null;
    }
    String blas = runVariables.get("BLAS");
    if(Strings.isNullOrEmpty(blas)) {
      return "unknown-blas";
    }
    return blas;
  }

  public String getJdk() {
    if(runVariables == null) {
      return null;
    }
    String jdk = runVariables.get("JDK");
    if(Strings.isNullOrEmpty(jdk)) {
      return "unknown-jdk";
    }
    return jdk;
  }

  public String getRunVariable(String name) {
    String value = null;
    if(runVariables != null) {
      value = runVariables.get(name);
    }
    return Strings.nullToEmpty(value);
  }
  
  public static Comparator<BenchmarkResult> comparator() {
    
    List<Comparator<BenchmarkResult>> comparators = new ArrayList<>();
    comparators.add(Ordering.natural().onResultOf(new Function<BenchmarkResult, String>() {
      @Override
      public String apply(BenchmarkResult input) {
        return input.getInterpreter();
      }
    }));
    comparators.add(Ordering.natural().onResultOf(new Function<BenchmarkResult, ArtifactVersion>() {
      @Override
      public ArtifactVersion apply(BenchmarkResult input) {
        return new DefaultArtifactVersion(input.getInterpreterVersion());
      }
    }));
    comparators.add(Ordering.natural().onResultOf(new Function<BenchmarkResult, Long>() {
      @Override
      public Long apply(BenchmarkResult input) {
        return input.getRunId();
      }
    }));
    
    return Ordering.compound(comparators);
  }

  public void setHarnessVersion(int harnessVersion) {
    this.harnessVersion = harnessVersion;
  }

  public int getHarnessVersion() {
    return harnessVersion;
  }

  public void setRunVariable(String name, String value) {
    if(runVariables == null) {
      runVariables = new HashMap<>();
    }
    runVariables.put(name, value);
  }
}
