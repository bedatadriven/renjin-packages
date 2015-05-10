package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.RenjinVersionId;

@Entity
public class RenjinVersionStat {

  
  /**
   * Composite identifier renjinVersion:deltaType
   */
  @Id
  private String id;
  
  

  @Unindex
  private int regressionCount;
 
  @Unindex
  private int progressionCount;

  public static String keyName(String renjinVersion, String statName) {
    return renjinVersion + ":" + statName;
  }
  
  public RenjinVersionStat() {
  }

  public String getId() {
    return id;
  }

  public String getRenjinVersion() {
    String[] parts = id.split(":");
    return parts[0];
  }


  public String getName() {
    String[] parts = id.split(":");
    return parts[1];
  }
  
  
  public RenjinVersionId getRenjinVersionId() {
    return new RenjinVersionId(getRenjinVersion());
  }
  
  public void setId(String id) {
    this.id = id;
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

}
