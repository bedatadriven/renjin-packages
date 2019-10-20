package org.renjin.ci.gradle.graph;

import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.lang.String.format;


public class PackageNode implements Serializable {

  private final PackageVersionId packageVersionId;

  /**
   * Dependencies of this node that are to be built during
   * this workflow.
   */
  private Future<Set<PackageNode>> dependencies;

  /**
   * true if we are reusing an existing build.
   */
  private boolean replaced;

  private String replacedVersion;



  public PackageNode(PackageVersionId packageVersionId, Future<Set<PackageNode>> dependencies) {
    this.packageVersionId = packageVersionId;
    this.dependencies = dependencies;
  }


  public PackageVersionId getId() {
    return packageVersionId;
  }

  @Override
  public String toString() {
    return packageVersionId.toString();
  }

  public boolean isReplaced() {
    if(packageVersionId.getPackageName().equals("testthat") ||
      packageVersionId.getPackageName().equals("Rcpp")) {
      return false;
    }
    return replaced;
  }

  public Set<PackageNode> getDependencies() {
    try {
      return dependencies.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public void replaced(String version) {
    this.replaced = true;
    this.replacedVersion = version;
  }

  public String getReplacedVersion() {
    return replacedVersion;
  }
}
