package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Date;

/**
 * Describes a single machine on which the benchmarks run
 */
@Entity
public class BenchmarkMachine {
  
  @Id
  private String id;
  
  @Unindex
  private String cpuInfo;
  
  @Unindex
  private String operatingSystem;

  @Unindex
  private Date lastUpdated;
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCpuInfo() {
    return cpuInfo;
  }

  public void setCpuInfo(String cpuInfo) {
    this.cpuInfo = cpuInfo;
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
  
  public String getPath() {
    return "/benchmarks/machine/" + id;
  }
}
