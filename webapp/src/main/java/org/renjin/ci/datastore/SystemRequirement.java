package org.renjin.ci.datastore;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Describes the latest available version of a "SystemRequirement"
 */
@Entity
public class SystemRequirement {

  @Id
  private String name;

  private String version;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
