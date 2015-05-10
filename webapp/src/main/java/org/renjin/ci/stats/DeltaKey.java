package org.renjin.ci.stats;

import org.renjin.ci.model.DeltaType;
import org.renjin.ci.model.RenjinVersionId;

import java.io.Serializable;

public class DeltaKey implements Serializable {
  private RenjinVersionId renjinVersionId;
  private DeltaType type;

  public DeltaKey(RenjinVersionId renjinVersionId, DeltaType type) {
    this.renjinVersionId = renjinVersionId;
    this.type = type;
  }

  public RenjinVersionId getRenjinVersionId() {
    return renjinVersionId;
  }

  public void setRenjinVersionId(RenjinVersionId renjinVersionId) {
    this.renjinVersionId = renjinVersionId;
  }

  public DeltaType getType() {
    return type;
  }

  public void setType(DeltaType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return renjinVersionId + ":" + type;
  }
}
