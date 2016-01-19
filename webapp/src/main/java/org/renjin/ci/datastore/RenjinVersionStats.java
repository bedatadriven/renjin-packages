package org.renjin.ci.datastore;


import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Date;
import java.util.List;

@Entity
public class RenjinVersionStats {
  
  public static final String KEY_NAME = "stats";
  
  public static Key<RenjinVersionStats> singletonKey() {
    return Key.create(RenjinVersionStats.class, KEY_NAME);
  }
  
  @Id
  private String id = KEY_NAME;
  
  private List<RenjinVersionStat> versions;

  @Unindex
  private Date updateTime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<RenjinVersionStat> getVersions() {
    return versions;
  }

  public void setVersions(List<RenjinVersionStat> versions) {
    this.versions = versions;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }
}

