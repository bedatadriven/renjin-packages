package org.renjin.ci.qa.compare;

import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.NoSuchObjectException;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import org.renjin.ci.model.VersionComparison;
import org.renjin.ci.model.VersionComparisonReport;

public class ComparisonViewModel {

  private VersionComparison comparison;
  private JobInfo jobInfo;
  private VersionComparisonReport report;

  public ComparisonViewModel(VersionComparison comparison) {
    this.comparison = comparison;

    if(comparison.getReport() != null) {
      report = comparison.getReport().get();
      if(report != null) {
        try {
          jobInfo = PipelineServiceFactory.newPipelineService().getJobInfo(report.getJobId());
        } catch (NoSuchObjectException e) {
          // no job info
        }
      }
    }
  }


  public JobInfo getJobInfo() {
    return jobInfo;
  }

  public String getFromVersion() {
    return comparison.getFromVersion().toString();
  }

  public String getToVersion() {
    return comparison.getToVersion().toString();
  }

  public java.util.Set<java.util.Map.Entry<String, Long>> getStatistics() {
    return report.getStatistics().entrySet();
  }

  public boolean isComplete() {
    if(report != null && report.isComplete()) {
      return report.isComplete();
    } else {
      return false;
    }
  }

  public boolean isRunning() {
    return !isComplete() && jobInfo != null && jobInfo.getJobState() == JobInfo.State.RUNNING;
  }

  public long getBuildRegressionCount() {
    return report.getStatistic(VersionComparisonReport.Statistic.BUILD_REGRESSIONS);
  }



  public long getBuildFixCount() {
    return report.getStatistic(VersionComparisonReport.Statistic.BUILD_FIXES);
  }
}
