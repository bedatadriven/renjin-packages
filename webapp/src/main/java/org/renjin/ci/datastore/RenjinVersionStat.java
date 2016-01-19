package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.RenjinVersionId;

import java.io.Serializable;

public class RenjinVersionStat implements Serializable {

  
  @Unindex
  private String renjinVersion;
  
  @Unindex
  private String name;
  
  @Unindex
  private int regressionCount;
 
  @Unindex
  private int progressionCount;
  
  public RenjinVersionStat() {
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RenjinVersionId getRenjinVersionId() {
    return new RenjinVersionId(renjinVersion);
  }
  

  public int getRegressionCount() {
    return regressionCount;
  }

  public void setRegressionCount(int regressionCount) {
    this.regressionCount = regressionCount;
  }

  public int getProgressionCount() {
    return progressionCount;
  }

  public void setProgressionCount(int progressionCount) {
    this.progressionCount = progressionCount;
  }

  public void setRenjinVersion(RenjinVersionId renjinVersionId) {
    this.renjinVersion = renjinVersionId.toString();
  }
}
