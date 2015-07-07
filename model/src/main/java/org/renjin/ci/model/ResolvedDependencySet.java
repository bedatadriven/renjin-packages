package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Set of package names resolved to fully qualified source names
 * or builds if available.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolvedDependencySet {
  
  private List<ResolvedDependency> dependencies = new ArrayList<>();

  public ResolvedDependencySet() {
  }

  public List<ResolvedDependency> getDependencies() {
    return dependencies;
  }
  
}
