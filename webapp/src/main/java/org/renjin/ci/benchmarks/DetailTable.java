package org.renjin.ci.benchmarks;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.BenchmarkResult;

import java.util.*;


public class DetailTable {
  
  public final List<BenchmarkResult> rows = new ArrayList<>();
  
  private List<String> variables = new ArrayList<>();
  
  public DetailTable(List<BenchmarkResult> results) {
    
    Set<String> variableSet = new HashSet<>();
    for (BenchmarkResult result : results) {
      if(result.getHarnessVersion() >= 4) {
        this.rows.add(result);
        if (result.getRunVariables() != null) {
          variableSet.addAll(result.getRunVariables().keySet());
        }
      }
    }
    this.variables = Lists.newArrayList(variableSet);
    Collections.sort(this.variables);
  }

  public List<String> getVariables() {
    return variables;
  }

  public List<BenchmarkResult> getRows() {
    return rows;
  }
}
