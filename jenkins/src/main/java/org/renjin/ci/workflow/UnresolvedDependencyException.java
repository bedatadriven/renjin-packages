package org.renjin.ci.workflow;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Failure raised when an unqualified package name (like 'MASS') cannot be resolved to
 * a qualified package version (like 'org.renjin.cran:MASS:3.2')
 */
public class UnresolvedDependencyException extends RuntimeException {
  
  private List<String> unresolvedPackages;

  public UnresolvedDependencyException(Iterable<String> unresolvedPackages) {
    super("Unresolved Packages: " + Iterables.toString(unresolvedPackages));
    this.unresolvedPackages = Lists.newArrayList(unresolvedPackages);
  }

  public List<String> getUnresolvedPackages() {
    return unresolvedPackages;
  }
}
