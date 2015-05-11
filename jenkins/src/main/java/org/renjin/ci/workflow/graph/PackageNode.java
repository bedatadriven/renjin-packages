package org.renjin.ci.workflow.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Set;


public class PackageNode implements Serializable {
  
  private final PackageVersionId packageVersionId;
  
  private BuildOutcome buildOutcome = null;
  
  private NodeState state = NodeState.READY;
  
  private long buildNumber;

  /**
   * Dependencies of this node that are to be built during
   * this workflow.
   */
  private Set<PackageNode> dependencies = Sets.newHashSet();
  
  private Set<PackageNode> dependants = Sets.newHashSet();

  /**
   * True if we have begun resolving dependencies for this 
   * node. Resolution may still not be complete.
   */
  private boolean dependenciesResolved;

  /**
   * true if we are reusing an existing build.
   */
  private boolean provided;
  

  public PackageNode(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }


  public void resolveAsBuild(long buildNumber) {
    this.buildNumber = buildNumber;
    this.buildOutcome = BuildOutcome.SUCCESS;
    this.state = NodeState.BUILT;
  }

  public boolean isDependenciesResolved() {
    return dependenciesResolved;
  }

  public void setDependenciesResolved(boolean dependenciesResolved) {
    this.dependenciesResolved = dependenciesResolved;
  }

  public boolean isBuilt() {
    return buildNumber != 0;
  }
  
  public void dependsOn(PackageNode node) {
    dependencies.add(node);
    node.dependants.add(this);
    
    if(node.getState() != NodeState.BUILT) {
      state = NodeState.BLOCKED;
    }
  }

  public Set<PackageNode> getDependants() {
    return dependants;
  }

  void buildCompleted(long buildNumber, BuildOutcome outcome) {
    assertState(NodeState.READY);
    
    this.buildOutcome = outcome;
    this.buildNumber = buildNumber;
    this.state = NodeState.BUILT;
  }
  
  boolean tryUnblock() {
    assertState(NodeState.BLOCKED);
    
    for (PackageNode dependency : dependencies) {
      if(  dependency.state == NodeState.ORPHANED ||
          (dependency.state == NodeState.BUILT &&
              dependency.buildOutcome != BuildOutcome.SUCCESS)) {
        
        state = NodeState.ORPHANED;
        return true;
      }
    }
    
    for (PackageNode dependency : dependencies) {
      if(dependency.state != NodeState.BUILT) {
        return false;
      }
    }
    state = NodeState.READY;
    return true;
  }
  
  public PackageVersionId getId() {
    return packageVersionId;
  }

  @Whitelisted
  public String getLabel() {
    return packageVersionId.toString();
  }
  
  @Whitelisted
  public NodeState getState() {
    return state;
  }

  @Override
  public String toString() {
    return packageVersionId + "@" + state;
  }


  public void assertState(NodeState expectedState) {
    Preconditions.checkState(state == expectedState, "%s: Expected state to be %s, but was %s",
        packageVersionId, expectedState, state);
  }

  public PackageNode getDependency(String packageName) {
    for (PackageNode dependency : dependencies) {
      if(dependency.getId().getPackageName().equals(packageName)) {
        return dependency;
      }
    }
    throw new IllegalArgumentException(
      String.format("Cannot find dependency name '%s' of node '%s'. Dependencies include: %s", 
          packageName, packageVersionId, dependencies.toString()));
  }

  public BuildOutcome getBuildOutcome() {
    assertState(NodeState.BUILT);
    Preconditions.checkState(buildOutcome != null, "Build outcome in node %s was null", getId());
    
    return buildOutcome;
  }
  
  public void provideBuild(long buildNumber) {
    this.state = NodeState.BUILT;
    this.buildOutcome = BuildOutcome.SUCCESS;
    this.buildNumber = buildNumber;
    this.provided = true;
  }

  public boolean isProvided() {
    return provided;
  }

  public String getBuildVersion() {
    assertState(NodeState.BUILT);
    Preconditions.checkState(buildOutcome == BuildOutcome.SUCCESS, "%s: No successfully build, outcome was %s",
        getId(), buildOutcome);
    Preconditions.checkState(buildNumber != 0, "%s: Build number is unset");
    
    return new PackageBuildId(packageVersionId, buildNumber).getBuildVersion();
  }

  public Set<PackageNode> getDependencies() {
    return ImmutableSet.copyOf(dependencies);
  }

  public void orphan() {
    state = NodeState.ORPHANED;
  }

  public String getDebugLabel() {
    StringBuilder label = new StringBuilder();
    label.append(state.name());
    if(provided) {
      label.append(" [provided]");
    } else if(state == NodeState.BUILT) {
      label.append(" [").append(buildOutcome).append("]");
    }

    return label.toString();
  }
}
