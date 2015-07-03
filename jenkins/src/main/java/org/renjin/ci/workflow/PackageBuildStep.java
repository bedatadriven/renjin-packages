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


public class PackageBuildStep extends Builder implements SimpleBuildStep {

  private String filter;
  private Double sample;

  @DataBoundConstructor
  public PackageBuildStep(String filter, Double sample) {
    this.filter = filter;
    this.sample = sample;
  }

  public String getFilter() {
    return filter;
  }

  public Double getSample() {
    return sample;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

    String renjinVersion = RenjinCiClient.getLatestRenjinRelease().toString();

    listener.getLogger().println("Building package graph...");
    PackageGraph graph;
    try {
      graph = new PackageGraphBuilder(listener).add(filter, sample);
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
    Tasks.waitForTasks(listener, queueItems);
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
