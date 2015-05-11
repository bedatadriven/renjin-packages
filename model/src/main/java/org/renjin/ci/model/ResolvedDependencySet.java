package org.renjin.ci.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Set of package names resolved to fully qualified source names
 * or builds if available.
 */
public class ResolvedDependencySet {
  
  private boolean complete = true;
  private List<String> missingPackages = new ArrayList<>();
  private List<ResolvedDependency> dependencies = new ArrayList<>();

  public ResolvedDependencySet() {
  }

  public List<ResolvedDependency> getDependencies() {
    return dependencies;
  }

  public List<String> getMissingPackages() {
    return missingPackages;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }
}
