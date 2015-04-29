package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of package names resolved to fully qualified source names
 * or builds if available.
 */
public class ResolvedDependencySet {
  
  private Map<String, ResolvedDependency> packages = new HashMap<>();

  public ResolvedDependencySet() {
  }

  @JsonCreator
  public ResolvedDependencySet(Map<String, ResolvedDependency> packages) {
    this.packages = packages;
  }

  @JsonValue
  public Map<String, ResolvedDependency> getPackages() {
    return packages;
  }
}
