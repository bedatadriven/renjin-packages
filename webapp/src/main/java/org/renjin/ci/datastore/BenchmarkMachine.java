package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
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

  @Index
  private Date lastUpdated;
  
  @Unindex
  private long physicalMemory;
  
  @Unindex
  private long availableProcessors;
  
  @Unindex
  private String cpuModel;
  
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

  public long getPhysicalMemory() {
    return physicalMemory;
  }
  
  public String getPhysicalMemoryDescription() {
    if(physicalMemory <= 0) {
      return "Unknown RAM";
    }
    
    double kb = physicalMemory / 1024d;
    double mb = kb / 1024d;
    double gb = mb / 1024d;
    
    return String.format("%.0f GB RAM", gb);
  }

  public void setPhysicalMemory(long physicalMemory) {
    this.physicalMemory = physicalMemory;
  }

  public long getAvailableProcessors() {
    return availableProcessors;
  }

  public void setAvailableProcessors(long availableProcessors) {
    this.availableProcessors = availableProcessors;
  }

  public String getCpuModel() {
    return cpuModel;
  }

  public void setCpuModel(String cpuModel) {
    this.cpuModel = cpuModel;
  }
}
