package org.renjin.ci.workflow.graph;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Build Queue that maintains an ordered list of packages to build
 */
public class BuildQueue implements Serializable {

  private final PackageGraph graph;
  
  private int blockedPackages = 0;
  
  private final BlockingQueue<PackageNode> ready = new LinkedBlockingQueue<PackageNode>();

  BuildQueue(PackageGraph graph) {
    this.graph = graph;
    for (PackageNode packageNode : graph.getNodes()) {
      if(packageNode.getState() == NodeState.READY) {
        ready.add(packageNode);
      } else if(packageNode.getState() == NodeState.BLOCKED) {
        blockedPackages ++;
      }
    }
  }
  
  @Whitelisted
  public Lease take() throws InterruptedException {
    PackageNode toBuild = ready.take();

    toBuild.assertState(NodeState.READY);

    return new Lease(toBuild);
  }
  
  @Whitelisted
  public boolean isEmpty() {
    return blockedPackages == 0 && ready.isEmpty();
  }
  
  @Whitelisted
  public int getReadyCount() {
    return ready.size();
  }
  
  
  public class Lease implements Serializable {
    private final PackageNode node;

    public PackageVersionId getPackageVersionId() {
      return node.getId();
    }

    public Lease(PackageNode node) {
      this.node = node;
    }

    public PackageNode getNode() {
      return node;
    }

    @Override
    public String toString() {
      return node.getId().toString();
    }

    public void completed(TaskListener listener, long buildNumber, BuildOutcome outcome) {

      synchronized (graph) {

        listener.getLogger().println("Marking " + node.getId() + " as " + outcome);

        node.buildCompleted(buildNumber, outcome);

        for (PackageNode dependant : node.getDependants()) {
          if (dependant.getState() == NodeState.BLOCKED &&
              dependant.transitionToReady()) {

            listener.getLogger().println(dependant.getId() + " transitioned to READY");

            ready.add(dependant);
            blockedPackages--;
          } else {
            listener.getLogger().println(dependant.getId() + " still " + dependant.getState());
          }
        }
      }
    }
  }
}
