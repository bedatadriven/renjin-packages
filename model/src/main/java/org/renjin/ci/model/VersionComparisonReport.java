package org.renjin.ci.model;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.KeyRange;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Map;

@Entity
public class VersionComparisonReport {


  public enum Statistic {
    BUILD_REGRESSIONS,
    BUILD_FIXES
  }

  @Id
  private long id;

  @Unindex
  private String jobId;

  @Unindex
  private boolean complete;

  private Map<String, Long> statistics;

  public VersionComparisonReport() {
  }

  public VersionComparisonReport(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public Key<VersionComparisonReport> key() {
    return Key.create(VersionComparisonReport.class, id);
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  public Map<String, Long> getStatistics() {
    return statistics;
  }

  public long getStatistic(Statistic statistic) {
    Long value = statistics.get(statistic.name());
    if(value == null) {
      return 0L;
    } else {
      return value;
    }
  }

  public void setStatistics(Map<String, Long> statistics) {
    this.statistics = statistics;
  }

  public static long newReportId() {
    String kind = Key.getKind(VersionComparisonReport.class);
    KeyRange keys = DatastoreServiceFactory.getDatastoreService().allocateIds(kind, 1);
    return keys.getStart().getId();
  }
}
