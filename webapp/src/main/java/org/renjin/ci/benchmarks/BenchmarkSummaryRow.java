package org.renjin.ci.benchmarks;

import org.renjin.ci.datastore.BenchmarkSummary;
import org.renjin.ci.datastore.BenchmarkSummaryPoint;

public class BenchmarkSummaryRow {


  private String machineId;
  private BenchmarkSummary summary;
  private Cell gnu;
  private Cell renjin;
  
  public BenchmarkSummaryRow(String machineId, BenchmarkSummary summary) {
    this.machineId = machineId;
    this.summary = summary;
    if(summary.getInterpreters().get("GNU R") != null) {
      gnu = new Cell("GNU R");
    }
    if(summary.getInterpreters().get("Renjin") != null) {
      renjin = new Cell("Renjin");
    }
  }
  
  public String getName() {
    return summary.getBenchmarkName();
  }

  public Cell getGnu() {
    return gnu;
  }

  public Cell getRenjin() {
    return renjin;
  }
  
  public String getSpeedup() {
    if(gnu == null || renjin == null) {
      return "-";
    } 
    double speedUp = ((double) gnu.point.getMeanRunTime()) /
                     ((double) renjin.point.getMeanRunTime());
    
    return String.format("%.2f", speedUp);
  }
  
  public String getPath() {
    return "/benchmarks/machine/" + machineId + "/benchmark/" + summary.getBenchmarkName();
  }

  public BenchmarkSummary getSummary() {
    return summary;
  }

  public class Cell {
    private String interpreter;
    private BenchmarkSummaryPoint point;

    public Cell(String interpreter) {
      this.interpreter = interpreter;
      this.point = summary.getInterpreters().get(interpreter);
    }
    
    public String getTitle() {
      if(point == null) {
        return "";
      }
      return interpreter + " " + point.getInterpreterVersion();
    }
    
    public String getTime() {
      if(point == null) {
        return "";
      }
      return formatRunTime(point.getMeanRunTime());
    }

  }


  public static String formatRunTime(double millis) {
    if(millis < 5000) {
      return String.format("%.0f ms", millis);
    } else if(millis < 60_000) {
      return String.format("%.1f s", millis/1000d);
    } else {
      return String.format("%.1f min", millis/1000d/60d);
    }
  }

}
