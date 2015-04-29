package org.renjin.ci.workflow.graph;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.workflow.tools.WebApp;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a queue of packages to build, ensuring that dependency
 * order is maintained.
 */
public class PackageGraph implements Serializable {

  
  /**
   * Set of PackageNodes that are to be built during this run. 
   */
  private final Map<PackageVersionId, PackageNode> nodes = new HashMap<PackageVersionId, PackageNode>();


  public Collection<PackageNode> getNodes() {
    return nodes.values();
  }

  @Whitelisted
  public int size() {
    return nodes.size();
  }

  public void add(TaskListener listener, PackageVersionId packageVersionId) {
    getOrAddNode(listener, packageVersionId);
  }


  private PackageNode getOrAddNode(TaskListener taskListener, PackageVersionId packageVersionId) {
    PackageNode node = nodes.get(packageVersionId);
    if(node == null) {
      taskListener.getLogger().println("Adding package node " + packageVersionId);

      node = new PackageNode(packageVersionId);
      nodes.put(node.getId(), node);

      // Add dependencies 
      for (ResolvedDependency resolvedDependency : WebApp.resolveDependencies(packageVersionId)) {
        taskListener.getLogger().println(packageVersionId + " depends on " + resolvedDependency.getPackageVersionId());
        PackageNode adjacent = getOrAddNode(taskListener, resolvedDependency.getPackageVersionId());
        if(resolvedDependency.hasBuild()) {
          node.setBuildNumber(resolvedDependency.getBuildNumber());
        }
        node.dependsOn( adjacent);
      }
    }
    return node;
  }
  
  @Whitelisted
  public BuildQueue newBuildQueue() {
    return new BuildQueue(this);
  }
}
