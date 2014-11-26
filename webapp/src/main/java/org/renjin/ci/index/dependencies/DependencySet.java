package org.renjin.ci.index.dependencies;

import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Version dependency set
 */
public class DependencySet implements Serializable {

  private boolean compileDependenciesResolved;
  private Set<PackageVersionId> dependencies = new HashSet<>();

  public boolean isCompileDependenciesResolved() {
    return compileDependenciesResolved;
  }

  public void setCompileDependenciesResolved(boolean compileDependenciesResolved) {
    this.compileDependenciesResolved = compileDependenciesResolved;
  }

  public Set<PackageVersionId> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<PackageVersionId> dependencies) {
    this.dependencies = dependencies;
  }

  public void add(PackageVersionId packageVersionId) {
    dependencies.add(packageVersionId);
  }
}
