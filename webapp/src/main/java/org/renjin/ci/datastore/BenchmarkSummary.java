package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class BenchmarkSummary {
  
  @Parent
  private Key<BenchmarkMachine> parentKey;
  
  @Id
  private String benchmarkName;

  private Map<String, BenchmarkSummaryPoint> interpreters = new HashMap<>();

  private List<BenchmarkChangePoint> changePoints;

  /**
   * The Renjin version that introduced the regression
   */
  @Index
  private String regression;

  private Double regressionSize;

  public BenchmarkSummary() {
  }

  public BenchmarkSummary(String machineId, String benchmarkName) {
    this(parentKey(machineId), benchmarkName);
  }

  private static Key<BenchmarkMachine> parentKey(String machineId) {
    return Key.create(BenchmarkMachine.class, machineId);
  }

  public BenchmarkSummary(Key parentKey, String benchmarkName) {
    this.parentKey = parentKey;
    this.benchmarkName = benchmarkName;
  }

  public static Key<BenchmarkSummary> key(String machineId, String benchmarkId) {
    return Key.create(parentKey(machineId), BenchmarkSummary.class,  benchmarkId);
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

  /**
   *
   * @return the Renjin release that introduced a performance regression
   */
  public String getRegression() {
    return regression;
  }

  public void setRegression(String regression) {
    this.regression = regression;
  }

  public Double getRegressionSize() {
    return regressionSize;
  }

  public void setRegressionSize(Double regressionSize) {
    this.regressionSize = regressionSize;
  }

  public List<BenchmarkChangePoint> getChangePoints() {
    return changePoints;
  }

  public void setChangePoints(List<BenchmarkChangePoint> changePoints) {
    this.changePoints = changePoints;
  }

  public void addChangePoint(BenchmarkChangePoint changePoint) {
    if (this.changePoints == null) {
      this.changePoints = new ArrayList<>();
    }
    changePoints.add(changePoint);
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
