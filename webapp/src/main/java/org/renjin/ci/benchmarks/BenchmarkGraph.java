package org.renjin.ci.benchmarks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Graph the results of a single benchmark on a single machine, comparing different interpreters
 */
public class BenchmarkGraph {
  
  

  private final String name;
  private final Collection<BenchmarkResult> results;
  private final List<RenjinVersionId> renjinVersions;
  private List<Double> baselineTimings = Lists.newArrayList();
  private Map<RenjinVersionId, List<Double>> renjinTimings = Maps.newHashMap();

  public BenchmarkGraph(String name, Collection<BenchmarkResult> benchmarkResults, 
                        String baselineVersion, List<RenjinVersionId> renjinVersions) {
    this.name = name;
    this.results = benchmarkResults;
    this.renjinVersions = renjinVersions;

    for (RenjinVersionId renjinVersion : renjinVersions) {
      renjinTimings.put(renjinVersion, Lists.<Double>newArrayList());
    }
    
    for (BenchmarkResult result : benchmarkResults) {
      if(result.getInterpreter().equals("GNU R") && result.getInterpreterVersion().equals(baselineVersion)) {
        baselineTimings.add((double)result.getRunTime());
      } else if(result.getInterpreter().equals("Renjin")) {
        RenjinVersionId renjinVersionId = new RenjinVersionId(result.getInterpreterVersion());
        if(renjinTimings.containsKey(renjinVersionId)) {
          renjinTimings.get(renjinVersionId).add((double)result.getRunTime());
        }
      }
    }
  }
  
  public String getBaselineTiming() {
    return timing(baselineTimings);
  }
  
  public String getRenjinTiming(RenjinVersionId versionId) {
    return timing(renjinTimings.get(versionId));
  }

  public String getName() {
    return name;
  }

  public Collection<BenchmarkResult> getResults() {
    return results;
  }
  
  private String timing(Collection<Double> timings) {
    if(timings.isEmpty()) {
      return "-";
    }
    double mean = mean(timings);
    double variance = variance(timings, mean);
    double stdev = Math.sqrt(variance);
    
    if(timings.size() > 1) {
      return formatRunTime(mean) + " ± " + formatRunTime(stdev);
    } else {
      return formatRunTime(mean);
    }
  }
  
  private String formatRunTime(double millis) {
    if(millis < 5000) { 
      return String.format("%.0f ms", millis);
    } else if(millis < 60_000) {
      return String.format("%.1f s", millis/1000d);
    } else {
      return String.format("%.1f min", millis/1000d/60d);
    }
  }
  
  private double mean(Iterable<Double> timings) {
    double sum = 0;
    double count = 0;
    for (Double timing : timings) {
      if(timing != null && !Double.isNaN(timing)) {
        sum += timing;
        count++;
      }
    }
    return sum / count;
  }
  
  private double variance(Iterable<Double> timings, double mean) {
    double sumOfDeviances = 0;
    double count = 0;
    for (Double timing : timings) {
      if (timing != null && !Double.isNaN(timing)) {
        sumOfDeviances += (timing - mean) * (timing - mean);
        count++;
      }
    }
    return sumOfDeviances / count;
  }
}
