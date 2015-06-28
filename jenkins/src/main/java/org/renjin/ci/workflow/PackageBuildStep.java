package org.renjin.ci.workflow;

import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.workflow.graph.PackageGraph;
import org.renjin.ci.workflow.graph.PackageGraphBuilder;
import org.renjin.ci.workflow.graph.PackageNode;
import org.renjin.ci.workflow.graph.PackageNodeState;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class PackageBuildStep extends Builder implements SimpleBuildStep {


  @DataBoundConstructor
  public PackageBuildStep() {
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

    String renjinVersion = RenjinCiClient.getLatestRenjinRelease().toString();

    listener.getLogger().println("Building package graph...");
    PackageGraph graph;
    try {
      graph = new PackageGraphBuilder(listener).add("new", null);
    } catch (Exception e) {
      throw new AbortException("Failed to build package graph: " + e.getMessage());
    }

    // Queue each of the unbuilt nodes as a task
    List<Queue.WaitingItem> queueItems = Lists.newArrayList();
    for (PackageNode node : graph.getNodes()) {
      if(node.getBuildResult() == PackageNodeState.NOT_BUILT) {
        PackageBuildTask task = new PackageBuildTask(run, listener, renjinVersion, node);
        queueItems.add(Jenkins.getInstance().getQueue().schedule(task, 0));
      }
    }
    
    // Wait for everything to complete!
    try {
      for (Queue.WaitingItem queueItem : queueItems) {
        try {
          queueItem.getFuture().get();
        } catch (ExecutionException e) {
          listener.fatalError(queueItem.task.getName() + " failed: " + e.getMessage());
        }
      }
      listener.getLogger().println("Queue complete.");

    } catch (InterruptedException e) {
      for (Queue.WaitingItem waitingItem : queueItems) {
        if(!waitingItem.getFuture().isDone()) {
          Jenkins.getInstance().getQueue().cancel(waitingItem);
        }
      }      
    }
    
  }

  @Override
  public Descriptor<Builder> getDescriptor() {
    return new DescriptorImpl();
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<Builder> {
    public DescriptorImpl() {
    }

    public Builder newInstance(StaplerRequest req, JSONObject data) {
      return req.bindJSON(PackageBuildStep.class, data);
    }

    public String getDisplayName() {
      return "Build a set of R packages for Renjin";
    }
  }
}
