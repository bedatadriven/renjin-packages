package org.renjin.ci.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

/**
 * A running build job
 */
@Entity
public class BuildJob {

  @Id
  private long id;

  private String renjinVersion;

  private Date startDate;

  private boolean cancelled;

  private String pipelineJobId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getPipelineJobId() {
    return pipelineJobId;
  }

  public void setPipelineJobId(String pipelineJobId) {
    this.pipelineJobId = pipelineJobId;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
