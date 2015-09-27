package org.renjin.ci.stats;

import org.renjin.ci.model.PackageId;

import java.io.Serializable;

public class DeltaValue implements Serializable {
  
  private String key;
  private int delta;

  public DeltaValue(PackageId packageId, int delta) {
    this.key = packageId.toString();
    this.delta = delta;
  }
  
  public DeltaValue(PackageId packageId, String testName, int delta) {
    this.key = packageId.toString() + ":" + testName;
    this.delta = delta;
  }


  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public int getDelta() {
    return delta;
  }

  public void setDelta(int delta) {
    this.delta = delta;
  }
}
