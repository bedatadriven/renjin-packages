package org.renjin.build.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class Package {

  @Id
  private String id;

  private String latestVersion;

  private String title;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
