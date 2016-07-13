package org.renjin.ci.benchmarks;

import com.google.appengine.api.datastore.QueryResultIterable;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.datastore.PackageDatabase;

/**
 * Shows results for a single machine and benchmark
 */
public class DetailPage {
  
  
  public DetailPage(String machineId, String benchmarkId) {

    QueryResultIterable<BenchmarkResult> results =
        PackageDatabase.getBenchmarkResultsForMachine(machineId, benchmarkId).iterable();

    // Find the max time taken
    long maxTime = 0;
    for (BenchmarkResult result : results) {
      if (result.isCompleted() && result.getRunTime() > maxTime) {
        maxTime = result.getRunTime();
      }
    }

    // Now sort these into runs
    for (BenchmarkResult result : results) {

    }
  }
}
