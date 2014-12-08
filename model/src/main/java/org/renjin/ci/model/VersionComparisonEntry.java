package org.renjin.ci.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.condition.IfZero;

@Entity
public class VersionComparisonEntry {


  @Parent
  private Key<VersionComparisonReport> reportKey;

  @Id
  private String packageVersionId;

  @IgnoreSave(IfZero.class)
  private int buildDelta;

  public VersionComparisonEntry() {
  }

  public VersionComparisonEntry(VersionComparisonReport report, PackageVersionId packageVersionId) {
    this.reportKey = report.key();
    this.packageVersionId = packageVersionId.toString();
  }


  public VersionComparisonEntry(long reportId, PackageVersionId packageVersionId) {
    this(new VersionComparisonReport(reportId), packageVersionId);
  }

  public Key<VersionComparisonReport> getReportKey() {
    return reportKey;
  }

  public void setReportKey(Key<VersionComparisonReport> reportKey) {
    this.reportKey = reportKey;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public void setBuildDelta(Delta delta) {
    this.buildDelta = delta.getCode();
  }

  public int getBuildDelta() {
    return buildDelta;
  }
}
