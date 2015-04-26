package org.renjin.ci.benchmarks;


public class Submission {
    
    private String benchmarkId;
    private double value;

    /**
     * The timestamp of the results file, used to avoid double submissions 
     */
    private long time;


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public void setBenchmarkId(String benchmarkId) {
        this.benchmarkId = benchmarkId;
    }
}
