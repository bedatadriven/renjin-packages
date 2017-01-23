package org.renjin.ci.datastore;

import org.renjin.ci.benchmarks.BenchmarkSummaryRow;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.releases.ReleasesResource;

/**
 * Denotes a changepoint in the benchmark mean running time.
 */
public class BenchmarkChangePoint {

    private String version;
    private String previousVersion;
    private double mean;
    private double previousMean;

    public BenchmarkChangePoint() {
    }

    public String getVersion() {
        return version;
    }

    public RenjinVersionId getVersionId() {
        return RenjinVersionId.valueOf(version);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public double getMean() {
        return mean;
    }

    public String getMeanString() {
        return BenchmarkSummaryRow.formatRunTime(mean);
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public RenjinVersionId getPreviousVersionId() {
        return RenjinVersionId.valueOf(previousVersion);
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public double getPreviousMean() {
        return previousMean;
    }

    public void setPreviousMean(double previousMean) {
        this.previousMean = previousMean;
    }

    public String getDiffUrl() {
        return ReleasesResource.compareUrl(getPreviousVersionId(), getVersionId());
    }

    public double getPercentageChange() {
        return (Math.abs(previousMean - mean)) / previousMean * 100d;
    }

    public String getPercentageChangeString() {
        return formatPercentageChange(getPercentageChange());
    }

    public boolean isRegression() {
        return previousMean < mean;
    }

    public static String formatPercentageChange(double change) {
        if(change < 1) {
            return String.format("%.2f%%", change);
        } else if(change < 10) {
            return String.format("%.1f%%", change);
        } else {
            return String.format("%.0f%%", change);
        }
    }
}
