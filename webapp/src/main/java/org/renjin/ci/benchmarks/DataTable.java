package org.renjin.ci.benchmarks;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.renjin.ci.datastore.BenchmarkResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataTable {


  private final String array;
  private double scale;
  private String units;
  
  public DataTable(Iterable<BenchmarkResult> results, String trackInterpreter) {
    
    
    // Find the latest version of each intepreter
    Map<String, ArtifactVersion> latestVersionByTerp = new HashMap<>();
    for (BenchmarkResult result : results) {
      if(result.isCompleted()) {
        ArtifactVersion version = new DefaultArtifactVersion(result.getInterpreterVersion());
        ArtifactVersion latestVersion = latestVersionByTerp.get(result.getInterpreter());
        if(latestVersion == null || version.compareTo(latestVersion) > 0) {
          latestVersionByTerp.put(result.getInterpreter(), version);
        }
      }
    }
    
    // Find the maximum runtime and determine scale
    double maxRunTime = 0;
    for (BenchmarkResult result : results) {
      if(result.isCompleted()) {
        if(result.getRunTime() > maxRunTime) {
          maxRunTime = result.getRunTime();
        }
      }
    }
    if(maxRunTime < 1000) {
      scale = 1;
      units = "milliseconds";
    } else {
      double seconds = maxRunTime / 1000d;
      if(seconds < 120) {
        scale = 1000;
        units = "seconds";
      } else {
        scale = 1000 * 60;
        units = "minutes";
      }
    }
    
    // Compute means of the latest versions of other interpreters
    Map<String, Double> meanRunTimeByTerp = new HashMap<>();
    for (String terp : latestVersionByTerp.keySet()) {
      if (!terp.equals(trackInterpreter)) {
        double total = 0;
        double count = 0;
        String latestVersion = latestVersionByTerp.get(terp).toString();
        for (BenchmarkResult result : results) {
          if (result.getInterpreter().equals(terp) && result.getInterpreterVersion().equals(latestVersion)) {
            if (result.isCompleted()) {
              total += result.getRunTime();
              count++;
            }
          }
        }
        meanRunTimeByTerp.put(terp + " " + latestVersion, (total / count));
      }
    }
    
    List<String> referenceTerps = new ArrayList<>(meanRunTimeByTerp.keySet());

    StringBuilder array = new StringBuilder();
    array.append("[");
    array.append(quote(trackInterpreter + " Version"));
    array.append(", ");
    array.append(quote(trackInterpreter));
    for (String referenceTerp : referenceTerps) {
      array.append(", ");
      array.append(quote(referenceTerp));
    }
    array.append("]");

    for (BenchmarkResult result : results) {
      if(result.getInterpreter().equals(trackInterpreter) && result.isCompleted()) {
        array.append(",\n [");
        array.append(quote(result.getInterpreterVersion() + " #" + result.getRunId()));
        array.append(",");
        array.append(scale(result.getRunTime()));
        for (String referenceTerp : referenceTerps) {
          double meanTime = meanRunTimeByTerp.get(referenceTerp);
          array.append(",");
          array.append(scale(meanTime));
        }
        array.append("]");
      }
    }
    this.array = array.toString();
  }

  private String scale(double runTime) {
    return String.format("%.1f", runTime / scale);
  }

  private String quote(String s) {
    return "'" + s + "'";
  }

  public String getArray() {
    return array;
  }

  public double getScale() {
    return scale;
  }

  public String getUnits() {
    return units;
  }
}
