package org.renjin.ci.workflow.queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Set;


public class PackageNode implements Serializable {
  
  private final PackageVersionId packageVersionId;
  
  private NodeState state = NodeState.READY;

  /**
   * Dependencies of this node that are to be built during
   * this workflow.
   */
  private Set<PackageNode> dependencies = Sets.newHashSet();
  
  private Set<PackageNode> dependants = Sets.newHashSet();
  
  private Set<PackageBuildId> builtDependencies = Sets.newHashSet();


  public PackageNode(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }
  
  public void dependsOn(PackageBuildId packageBuildId) {
    builtDependencies.add(packageBuildId);
  }

  public void dependsOn(PackageNode node) {
    dependencies.add(node);
    node.dependants.add(this);
    
    // A dependency on an unbuilt node transitions this node
    // to 'BLOCKED'
    
    state = NodeState.BLOCKED;
    
  }
  
  public PackageVersionId getId() {
    return packageVersionId;
  }

  public NodeState getState() {
    return state;
  }

  public PackageNode lease() {
    Preconditions.checkState(state == NodeState.READY, getId() + " must be in READY state, currently in " + state);
    
    this.state = NodeState.LEASED;

    return this;
  }

  @Override
  public String toString() {
    return packageVersionId + "@" + state;
  }
}
