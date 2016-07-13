package org.renjin.ci.benchmarks;

import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.BenchmarkMachine;
import org.renjin.ci.datastore.BenchmarkSummary;
import org.renjin.ci.datastore.PackageDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * View model for benchmark results from a single machine
 */
public class MachinePage {

  private final LoadResult<BenchmarkMachine> machine;
  private final List<BenchmarkSummaryRow> rows = new ArrayList<>();

  public MachinePage(String machineId) {
    this.machine = PackageDatabase.getBenchmarkMachine(machineId);
    for (BenchmarkSummary summary : PackageDatabase.getBenchmarkSummaries(machineId)) {
      rows.add(new BenchmarkSummaryRow(summary));
    }
  }

  public BenchmarkMachine getMachine() {
    return machine.safe();
  }

  public List<BenchmarkSummaryRow> getBenchmarks() {
    return rows;
  }
}
