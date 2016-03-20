package org.renjin.ci.model;

import java.io.Serializable;

/**
 * Describes the machine on which benchmarks are run.
 * 
 * <p>We assume that the machine is static.</p>
 */
public class MachineDescriptor implements Serializable {
  
  private String id;
  
  
  private String operatingSystem;

  /**
   * Total amount of available physical memory, in bytes
   */
  private long physicalMemory;
  
  private int availableProcessors;

  private String cpuModel;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  public long getPhysicalMemory() {
    return physicalMemory;
  }

  public void setPhysicalMemory(long physicalMemory) {
    this.physicalMemory = physicalMemory;
  }
  

  public int getAvailableProcessors() {
    return availableProcessors;
  }

  public void setAvailableProcessors(int availableProcessors) {
    this.availableProcessors = availableProcessors;
  }

  public String getCpuModel() {
    return cpuModel;
  }

  public void setCpuModel(String cpuModel) {
    this.cpuModel = cpuModel;
  }
}
