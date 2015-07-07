package org.renjin.ci.jenkins;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.jenkins.graph.PackageGraph;
import org.renjin.ci.jenkins.graph.PackageGraphBuilder;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.graph.PackageNodeState;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class PackageBuildStep extends Builder implements SimpleBuildStep {

  private String filter;
  private Double sample;
  private String renjinVersion;
  
  @DataBoundConstructor
  public PackageBuildStep(String filter, Double sample, String renjinVersion) {
    this.filter = filter;
    this.sample = sample;
    this.renjinVersion = renjinVersion;
  }

  public String getFilter() {
    return filter;
  }

  public Double getSample() {
    return sample;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

    
    RenjinVersionId renjinVersion;
    if(Strings.isNullOrEmpty(this.renjinVersion) || this.renjinVersion.equals("LATEST")) {
      renjinVersion = RenjinCiClient.getLatestRenjinRelease();
    } else {
      renjinVersion = RenjinVersionId.valueOf(this.renjinVersion);
    }

    listener.getLogger().println("Building package graph...");
    PackageGraph graph;
    try {
      graph = new PackageGraphBuilder(listener).build(filter, sample);
    } catch (Exception e) {
      throw new AbortException("Failed to build package graph: " + e.getMessage());
    }
    
    // Create a list of packages to build, ordered by the number of downstream builds
    // That way, we maximize throughput by build the packages need the most first
    List<PackageNode> buildList = Lists.newArrayList(graph.getNodes());
    Collections.sort(buildList, Ordering.natural().onResultOf(new Function<PackageNode, Comparable>() {
      @Override
      public Comparable apply(PackageNode input) {
        return input.getDownstreamCount();
      }
    }).reverse());
    

    // Queue each of the unbuilt nodes as a task
    List<Queue.WaitingItem> queueItems = Lists.newArrayList();
    for (PackageNode node : buildList) {
      if(node.getBuildResult() == PackageNodeState.NOT_BUILT) {
        PackageBuildTask task = new PackageBuildTask(run, listener, renjinVersion.toString(), node);
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