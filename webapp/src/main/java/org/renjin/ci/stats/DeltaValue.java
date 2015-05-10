package org.renjin.ci.stats;

import org.renjin.ci.model.PackageId;

import java.io.Serializable;

public class DeltaValue implements Serializable {
  
  private PackageId packageId;
  private int delta;

  public DeltaValue(PackageId packageId, int delta) {
    this.packageId = packageId;
    this.delta = delta;
  }

  public PackageId getPackageId() {
    return packageId;
  }

  public void setPackageId(PackageId packageId) {
    this.packageId = packageId;
  }

  public int getDelta() {
    return delta;
  }

  public void setDelta(int delta) {
    this.delta = delta;
  }
}
