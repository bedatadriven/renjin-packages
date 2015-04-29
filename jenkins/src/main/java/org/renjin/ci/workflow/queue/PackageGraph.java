package org.renjin.ci.workflow.queue;

import com.google.common.collect.Queues;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.workflow.tools.WebApp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maintains a queue of packages to build, ensuring that dependency
 * order is maintained.
 */
public class PackageGraph implements Serializable {
  
  private final TaskListener taskListener;

  /**
   * Set of PackageNodes that are to be built during this run. 
   */
  private final Map<PackageVersionId, PackageNode> nodes = new HashMap<PackageVersionId, PackageNode>();

  /**
   * A queue of packages ready to be built
   */
  private final BlockingQueue<PackageNode> ready = new LinkedBlockingQueue<PackageNode>();
  
  public PackageGraph(TaskListener taskListener) {
    this.taskListener = taskListener;
  }

  public PackageNode take() throws InterruptedException {
    return ready.take();
  }
  
  
  public PackageNode node(PackageVersionId packageVersionId) {
    PackageNode node = nodes.get(packageVersionId);
    if(node == null) {
      taskListener.getLogger().println("Adding package node " + packageVersionId);

      node = new PackageNode(packageVersionId);
      nodes.put(node.getId(), node);

      // Add dependencies 
      for (ResolvedDependency resolvedDependency : WebApp.resolveDependencies(packageVersionId)) {
        if (resolvedDependency.hasBuild()) {
          taskListener.getLogger().println(packageVersionId + " depends on " + resolvedDependency.getBuildId());
          node.dependsOn(resolvedDependency.getBuildId());

        } else {
          taskListener.getLogger().println(packageVersionId + " depends on " + resolvedDependency.getPackageVersionId());
          node.dependsOn(node(resolvedDependency.getPackageVersionId()));
        }
      }
      
      if(node.getState() == NodeState.READY) {
        ready.add(node);
      }
    }
    return node;
  }

  public void add(PackageVersionId packageVersionId) {
    node(packageVersionId);
  }
  
}
