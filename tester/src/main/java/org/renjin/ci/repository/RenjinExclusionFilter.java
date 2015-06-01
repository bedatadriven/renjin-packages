package org.renjin.ci.repository;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.util.List;


/**
 * Filter to exclude Renjin core dependencies when resolving a package's 
 * transitive dependencies.
 */
public class RenjinExclusionFilter implements DependencyFilter{


  @Override
  public boolean accept(DependencyNode node, List<DependencyNode> parents) {

    if(isCoreDependency(node.getArtifact())) {
      return false;
    }

    // Exclude this library if it is a transitive dependency of a
    // a renjin core dependency
    for (DependencyNode parent : parents) {
      if(isCoreDependency(parent.getArtifact())) {
        return false;
      }
    }
    return true;
  }

  private boolean isCoreDependency(Artifact artifact) {
    return artifact.getGroupId().equals("org.renjin");
  }
}
