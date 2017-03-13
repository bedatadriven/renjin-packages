package org.renjin.ci.benchmarks;

import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.BenchmarkMachine;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.datastore.BenchmarkSummary;
import org.renjin.ci.datastore.PackageDatabase;

import java.util.Collections;
import java.util.List;

/**
 * Shows results for a single machine and benchmark
 */
public class DetailPage {
  private String machineId;
  private String benchmarkId;
  private final LoadResult<BenchmarkMachine> machine;
  private final LoadResult<BenchmarkSummary> summary;
  private final DetailTable detailTable;
  private final List<BenchmarkResult> results;


  public DetailPage(String machineId, String benchmarkId) {
    this.machineId = machineId;
    this.benchmarkId = benchmarkId;

    this.machine = PackageDatabase.getBenchmarkMachine(machineId);
    this.summary = PackageDatabase.getBenchmarkSummary(machineId, benchmarkId);
    
    results = PackageDatabase.getBenchmarkResultsForMachine(machineId, benchmarkId).list();

    Collections.sort(results, BenchmarkResult.comparator());

    this.detailTable = new DetailTable(results);
  }


  public BenchmarkSummary getSummary() {
    return summary.safe();
  }

  public DetailTable getDetailTable() {
    return detailTable;
  }

  public String getBenchmarkId() {
    return benchmarkId;
  }

  public List<BenchmarkResult> getResults() {
    return results;
  }

  public BenchmarkMachine getMachine() {
    return machine.now();
  }

  public String getResultsPath() {
    return "/benchmarks/machine/" + machineId + "/benchmark/" + benchmarkId + ".csv";
  }
}
