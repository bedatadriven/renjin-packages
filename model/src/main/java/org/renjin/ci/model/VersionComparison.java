package org.renjin.ci.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * Stores the results of a comparison between two versions
 */
@Entity
public class VersionComparison {

  @Id
  private String id;


  /**
   * Id of the job currently building this comparison
   */
  @Unindex
  private Ref<VersionComparisonReport> report;


  public static Key<VersionComparison> key(RenjinVersionId from, RenjinVersionId to) {
    return Key.create(VersionComparison.class, from + ":" + to);
  }

  public VersionComparison() {
  }

  public VersionComparison(RenjinVersionId from, RenjinVersionId to) {
    this.id = key(from, to).getName();
  }

  public RenjinVersionId getFromVersion() {
    String[] versions = id.split(":");
    return new RenjinVersionId(versions[0]);
  }

  public RenjinVersionId getToVersion() {
    String[] versions = id.split(":");
    return new RenjinVersionId(versions[1]);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Ref<VersionComparisonReport> getReport() {
    return report;
  }

  public void setReport(Ref<VersionComparisonReport> report) {
    this.report = report;
  }

}
