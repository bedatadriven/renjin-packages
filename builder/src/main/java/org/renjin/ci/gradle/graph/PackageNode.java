package org.renjin.ci.gradle.graph;

import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.lang.String.format;


public class PackageNode implements Serializable {

  /**
   *
   */
  int mark = 0;
  
  private final PackageVersionId packageVersionId;
  
  private PackageNodeState buildResult = PackageNodeState.NOT_BUILT;

  /**
   * Dependencies of this node that are to be built during
   * this workflow.
   */
  private Future<Set<PackageNode>> dependencies;

  /**
   * true if we are reusing an existing build.
   */
  private boolean provided;


  public PackageNode(PackageVersionId packageVersionId, Future<Set<PackageNode>> dependencies) {
    this.packageVersionId = packageVersionId;
    this.dependencies = dependencies;
  }

  public boolean isBuilt() {
    return buildResult.isBuilt();
  }


  public PackageVersionId getId() {
    return packageVersionId;
  }

  public String getLabel() {
    return packageVersionId.toString();
  }
  
  @Override
  public String toString() {
    return packageVersionId.toString();
  }

  public void provideBuild(long buildNumber, BuildOutcome outcome) {
    this.buildResult = PackageNodeState.build(packageVersionId, buildNumber, outcome);
    this.provided = true;
  }

  public boolean isProvided() {
    if(packageVersionId.getPackageName().equals("testthat") ||
      packageVersionId.getPackageName().equals("Rcpp")) {
      return false;
    }
    return provided;
  }

  public boolean waitForDependencies() throws ExecutionException, InterruptedException {
    if(dependencies.isDone()) {
      return false;
    } else {
      dependencies.get();
      return true;
    }
  }
  
  public Set<PackageNode> getDependencies() {
    try {
      return dependencies.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDebugLabel() {
    return packageVersionId + "[" + buildResult + "]";
  }

  public PackageNodeState getBuildResult() {
    return buildResult;
  }

  public void replaced(String version) {
    this.buildResult = new PackageNodeState(version, BuildOutcome.SUCCESS);
    this.provided = true;
  }

  public void completed(String buildVersion, BuildOutcome outcome) {
    this.buildResult = new PackageNodeState(buildVersion, outcome);
  }

  public void completed(long buildNumber, BuildOutcome outcome) {
    this.buildResult = new PackageNodeState(packageVersionId, buildNumber, outcome);
  }

  public void cancelled() {
    this.buildResult = PackageNodeState.CANCELLED;
  }

  public void crashed() {
    this.buildResult = PackageNodeState.ERROR;
  }

  public void blocked(long buildNumber) {
    this.buildResult = new PackageNodeState(packageVersionId, buildNumber, BuildOutcome.BLOCKED);
  }

}
