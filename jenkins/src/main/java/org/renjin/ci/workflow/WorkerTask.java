package org.renjin.ci.workflow;

import com.google.common.base.Preconditions;
import hudson.model.*;
import hudson.model.queue.AbstractQueueTask;
import org.renjin.ci.workflow.graph.BuildQueue;

import java.io.IOException;

/**
 * 
 */
public class WorkerTask extends AbstractQueueTask {

  private int id;
  private Run<?, ?> run;  
  private TaskListener taskListener;
  private BuildQueue buildQueue;
  private String renjinVersion;

  public WorkerTask(int id, Run<?, ?> run, TaskListener taskListener, BuildQueue buildQueue, String renjinVersion) {
    this.id = id;
    this.run = run;
    this.buildQueue = buildQueue;
    this.renjinVersion = renjinVersion;
    this.taskListener = taskListener;

    Preconditions.checkNotNull(renjinVersion, "renjinVersion cannot be null");
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public BuildQueue getBuildQueue() {
    return buildQueue;
  }

  @Override
  public boolean isBuildBlocked() {
    return false;
  }

  @Override
  public String getWhyBlocked() {
    return null;
  }

  public int getId() {
    return id;
  }

  @Override
  public void checkAbortPermission() {
  
  }

  @Override
  public boolean hasAbortPermission() {
    return false;
  }

  @Override
  public String getUrl() {
    return "http://renjinci.appspot.com/";
  }

  @Override
  public ResourceList getResourceList() {
    return new ResourceList();
  }

  @Override
  public String getName() {
    return "Renjin Package Builder #" + getId();
  }


  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
  public String getFullDisplayName() {
    return getDisplayName();
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
    return new WorkerExecutable(this, taskListener);
   
  }

  public Run<?, ?> getRun() {
    return run;
  }

  @Override
  public String toString() {
    return getName();
  }

}
