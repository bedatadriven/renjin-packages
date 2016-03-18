package org.renjin.ci.benchmarks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.Query;
import org.renjin.ci.datastore.BenchmarkMachine;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * View model for benchmark results from a single machine
 */
public class MachinePage {

  private final LoadResult<BenchmarkMachine> machine;
  private final List<BenchmarkGraph> benchmarks = Lists.newArrayList();
  private final List<RenjinVersionId> renjinVersions;
  private final String baselineVersion;

  public MachinePage(String machineId) {

    this.machine = PackageDatabase.getBenchmarkMachine(machineId);

    // Set the version of GNU to use as a baseline
    baselineVersion = "3.2.4";
    
    // Group the results by benchmark,
    // make a list of all renjin builds available
    Set<RenjinVersionId> renjinVersions = Sets.newHashSet();
    Multimap<String, BenchmarkResult> resultMap = HashMultimap.create();
    Query<BenchmarkResult> results = PackageDatabase.getBenchmarkResultsForMachine(machineId);
    for (BenchmarkResult result : results) {
      if(result.getInterpreter().equals("Renjin")) {
        renjinVersions.add(new RenjinVersionId(result.getInterpreterVersion()));
      }
      resultMap.put(result.getBenchmarkName(), result);
    }
    
    // Sort the renjin versions by increasing order
    this.renjinVersions = Lists.newArrayList(renjinVersions);
    Collections.sort(this.renjinVersions);
    
    for (String benchmarkName : resultMap.keySet()) {
      benchmarks.add(new BenchmarkGraph(benchmarkName, resultMap.get(benchmarkName), 
          baselineVersion, this.renjinVersions));
    }
  }

  public String getBaselineVersion() {
    return baselineVersion;
  }
  
  public BenchmarkMachine getMachine() {
    return machine.safe();
  }

  public List<BenchmarkGraph> getBenchmarks() {
    return benchmarks;
  }

  public List<RenjinVersionId> getRenjinVersions() {
    return renjinVersions;
  }
}
