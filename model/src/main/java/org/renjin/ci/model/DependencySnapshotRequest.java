package org.renjin.ci.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request to resolve a set of dependencies.
 */
public class DependencySnapshotRequest {

  private String beforeDate;
  private final List<String> dependencies = new ArrayList<>();

  public String getBeforeDate() {
    return beforeDate;
  }

  public void setBeforeDate(String beforeDate) {
    this.beforeDate = beforeDate;
  }

  public List<String> getDependencies() {
    return dependencies;
  }
}
