package org.renjin.ci.jenkins.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;


public class PackageNode implements Serializable {
  
  private final PackageVersionId packageVersionId;
  
  private PackageNodeState buildResult = PackageNodeState.NOT_BUILT;

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

  private int downstreamCount;


  public PackageNode(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }
  
  
  public boolean isBuilt() {
    return buildResult.isBuilt();
  }
  
  public void dependsOn(PackageNode node) {
    dependencies.add(node);
    node.dependants.add(this);
  }
  
  public Set<PackageNode> getDependants() {
    return dependants;
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

  public void provideBuild(long buildNumber) {
    this.buildResult = PackageNodeState.success(buildNumber);
    this.provided = true;
  }

  public boolean isProvided() {
    return provided;
  }
  
  public Set<PackageNode> getDependencies() {
    return ImmutableSet.copyOf(dependencies);
  }

  public String getDebugLabel() {
    return packageVersionId + "[" + buildResult + "]";
  }

  public void orphan() {
    this.buildResult = PackageNodeState.ORPHANED;
  }

  public PackageNodeState getBuildResult() {
    return buildResult;
  }

  public PackageBuildId getDependency(String name) {
    for (PackageNode dependency : dependencies) {
      if(dependency.getId().getPackageName().equals(name)) {
        return dependency.getBuildId();
      }
    }
    throw new IllegalStateException(format("Package %s has no dependency named '%s'", getId(), name));
  }

  private PackageBuildId getBuildId() {
    PackageNodeState result = this.buildResult;
    if(result.getOutcome() == BuildOutcome.SUCCESS) {
      return new PackageBuildId(packageVersionId, result.getBuildNumber());
    } else {
      throw new IllegalStateException(format("Cannot return build id for %s, state is %s", 
          packageVersionId, buildResult));
    }
  }

  public void completed(long buildNumber, BuildOutcome outcome) {
    this.buildResult = new PackageNodeState(buildNumber, outcome);
  }

  public void cancelled() {
    this.buildResult = PackageNodeState.CANCELLED;
  }

  public void crashed() {
    this.buildResult = new PackageNodeState(-1, BuildOutcome.ERROR);
  }

  public List<String> resolvedDependencies() {
    List<String> list = new ArrayList<String>();
    for (PackageNode dependency : dependencies) {
      list.add(dependency.getId().toString());
    }
    return list;
  }

  public void blocked(long buildNumber) {
    this.buildResult = new PackageNodeState(buildNumber, BuildOutcome.BLOCKED);
  }

  public List<String> blockingDependencies() {
    List<String> list = new ArrayList<String>();
    for (PackageNode dependencyNode : dependencies) {
      PackageNodeState upstreamResult = dependencyNode.getBuildResult();
      if(upstreamResult.getOutcome() != BuildOutcome.SUCCESS) {
        if(upstreamResult.getBuildNumber() > 0) {
          PackageBuildId upstreamBuildId = new PackageBuildId(dependencyNode.getId(), upstreamResult.getBuildNumber());
          list.add(upstreamBuildId.toString());
        } else {
          list.add(dependencyNode.getId().toString());
        }
      }
    }
    return list;
  }

  /**
   * Computes the number of downstream builds 
   */
  public void computeDownstream() {
    Set<PackageNode> visited = new HashSet<PackageNode>();
    computeDownstream(visited);
    this.downstreamCount = visited.size() - 1;
  }

  private void computeDownstream(Set<PackageNode> visited) {
    if(visited.add(this)) {
      for (PackageNode dependant : dependants) {
        dependant.computeDownstream(visited);
      }
    }
  }

  public int getDownstreamCount() {
    return downstreamCount;
  }
}
