package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import java.util.HashMap;
import java.util.Map;

@Entity
public class BenchmarkSummary {
  
  @Parent
  private Key<BenchmarkMachine> parentKey;
  
  @Id
  private String benchmarkName;
  
  private Map<String, BenchmarkSummaryPoint> interpreters = new HashMap<>();

  public BenchmarkSummary() {
  }

  public BenchmarkSummary(String machineId, String benchmarkName) {
    this(Key.create(BenchmarkMachine.class, machineId), benchmarkName);    
  }
  
  public BenchmarkSummary(Key parentKey, String benchmarkName) {
    this.parentKey = parentKey;
    this.benchmarkName = benchmarkName;
  }

  public Key getParentKey() {
    return parentKey;
  }

  public void setParentKey(Key parentKey) {
    this.parentKey = parentKey;
  }

  public String getBenchmarkName() {
    return benchmarkName;
  }

  public void setBenchmarkName(String benchmarkName) {
    this.benchmarkName = benchmarkName;
  }

  public Map<String, BenchmarkSummaryPoint> getInterpreters() {
    return interpreters;
  }

  public void setInterpreters(Map<String, BenchmarkSummaryPoint> interpreters) {
    this.interpreters = interpreters;
  }

  @Override
  public String toString() {
    return "BenchmarkSummary{" +
        "parentKey=" + parentKey +
        ", benchmarkName='" + benchmarkName + '\'' +
        ", interpreters=" + interpreters +
        '}';
  }
}
