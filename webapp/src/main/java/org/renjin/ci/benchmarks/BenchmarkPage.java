package org.renjin.ci.benchmarks;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.BenchmarkMachine;
import org.renjin.ci.datastore.PackageDatabase;

import java.util.ArrayList;

/**
 * Index page model for benchmarks
 */
public class BenchmarkPage {

  private final Iterable<BenchmarkMachine> machines;

  public BenchmarkPage() {
    machines = PackageDatabase.getMostRecentBenchmarkMachines().iterable();
  }
  
  public ArrayList<BenchmarkMachine> getMachines() {
    return Lists.newArrayList(machines);
  }
  
}
