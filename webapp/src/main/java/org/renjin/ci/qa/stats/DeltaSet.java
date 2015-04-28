package org.renjin.ci.qa.stats;


import org.renjin.ci.model.RenjinVersionId;

import java.io.Serializable;

public class DeltaSet implements Serializable {
  private RenjinVersionId renjinVersionId;
  private String type;
  private int regressionCount;
  private int progressionCount;

  public DeltaSet(RenjinVersionId renjinVersionId, String type, int regressionCount, int progressionCount) {
    this.renjinVersionId = renjinVersionId;
    this.type = type;
    this.regressionCount = regressionCount;
    this.progressionCount = progressionCount;
  }

  public RenjinVersionId getRenjinVersionId() {
    return renjinVersionId;
  }

  public String getType() {
    return type;
  }

  public int getRegressionCount() {
    return regressionCount;
  }

  public int getProgressionCount() {
    return progressionCount;
  }
}
