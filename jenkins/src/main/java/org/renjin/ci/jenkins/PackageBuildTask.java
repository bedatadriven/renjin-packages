package org.renjin.ci.jenkins;

import hudson.model.*;
import hudson.model.queue.AbstractQueueTask;
import hudson.model.queue.CauseOfBlockage;
import hudson.security.Permission;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.graph.PackageNodeState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PackageBuildTask extends AbstractQueueTask {
  
  private final Run<?, ?> run;
  private final PackageNode packageNode;
  private final String renjinVersion;
  private final TaskListener listener;


  public PackageBuildTask(Run<?, ?> run, TaskListener listener, String renjinVersion, PackageNode packageNode) {
    this.run = run;
    this.listener = listener;
    this.renjinVersion = renjinVersion;
    this.packageNode = packageNode;
  }

  @Override
  public Label getAssignedLabel() {
    return Label.get("renjin-package-builder");
  }

  @Override
  public Node getLastBuiltOn() {
    return null;
  }

  @Override
  public long getEstimatedDuration() {
    return -1;
  }

  @Override
  public Queue.Executable createExecutable() throws IOException {
    return new PackageBuildExecutable(run, listener, this);
  }

  @Override
  public boolean isBuildBlocked() {
    return getCauseOfBlockage() != null;
  }

  @Override
  public String getWhyBlocked() {
    CauseOfBlockage causeOfBlockage = getCauseOfBlockage();
    if(causeOfBlockage == null) {
      return null;
    } else {
      return causeOfBlockage.getShortDescription();
    }
  }

  @Override
  public CauseOfBlockage getCauseOfBlockage() {
    
    // are all of our upstream dependencies built?
    List<PackageNode> blocking = new ArrayList<PackageNode>();
    for (PackageNode dependencyNode : packageNode.getDependencies()) {
      if (dependencyNode.getBuildResult() == PackageNodeState.NOT_BUILT) {
        blocking.add(dependencyNode);
      }
    }
    
    if(blocking.isEmpty()) {
      return null;
    } else {
      return new BecauseOfDependencies(blocking);
    }
  }
  
  @Override
  public String getName() {
    return packageNode.getLabel();
  }

  @Override
  public String getFullDisplayName() {
    return getName();
  }

  @Override
  public void checkAbortPermission() {
    run.checkPermission(Permission.DELETE); 
  }

  @Override
  public boolean hasAbortPermission() {
    return run.hasPermission(Permission.DELETE);
  }

  @Override
  public String getUrl() {
    return run.getUrl();
  }

  @Override
  public ResourceList getResourceList() {
    return ResourceList.EMPTY;
  }

  @Override
  public String getDisplayName() {
    return getName();
  }

  public TaskListener getListener() {
    return listener;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public PackageVersionId getPackageVersionId() {
    return packageNode.getId();
  }

  public PackageNode getPackageNode() {
    return packageNode;
  }

  private static class BecauseOfDependencies extends CauseOfBlockage {

    private final List<PackageNode> dependencies;

    public BecauseOfDependencies(List<PackageNode> dependencies) {
      this.dependencies = dependencies;
    }

    @Override
    public String getShortDescription() {
      return  "Waiting on dependencies...";
    }
  }
}
