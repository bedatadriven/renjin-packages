package org.renjin.ci.workflow.graph;

import com.google.common.base.Optional;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
  public Optional<Lease> take() {
    while(!isEmpty()) {
      PackageNode toBuild;
      try {
        toBuild = ready.poll(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        return Optional.absent();
      }
      if(toBuild != null) {
        toBuild.assertState(NodeState.READY);
        return Optional.of(new Lease(toBuild));
      }
    }
    
    return Optional.absent();
  }
  
  @Whitelisted
  public boolean isEmpty() {
    return blockedPackages == 0 && ready.isEmpty();
  }
  
  @Whitelisted
  public int getReadyCount() {
    return ready.size();
  }

  public int getTotalLength() {
    return blockedPackages + ready.size();
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

        transitionDependants(node, listener);
      }
    }

    private void transitionDependants(PackageNode parent, TaskListener listener) {
      for (PackageNode dependant : parent.getDependants()) {
        if (dependant.getState() == NodeState.BLOCKED &&
            dependant.tryUnblock()) {

          listener.getLogger().println(dependant.getId() + " transitioned to " + dependant.getState());

          if(dependant.getState() == NodeState.READY) {
            ready.add(dependant);
          } else if(dependant.getState() == NodeState.ORPHANED) {
            transitionDependants(dependant, listener);
          }
          blockedPackages--;
        } else {
          listener.getLogger().println(dependant.getId() + " still " + dependant.getState());
        }
      }
    }

    /**
     * Called when the build failed due to infrastructure problems.
     */
    public void crashed() {
      // add back onto the queue
      ready.add(node);
      
    }
  }
}
