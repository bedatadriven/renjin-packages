package org.renjin.ci.jenkins;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.renjin.ci.jenkins.graph.PackageGraph;
import org.renjin.ci.jenkins.graph.PackageGraphBuilder;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.graph.TopologicalSort;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.PullBuildId;
import org.renjin.ci.model.RenjinVersionId;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * Builds and tests a set of R packages against a development version of Renjin.
 *
 * <p>In contrast to the PackageBuildStep, all packages are built on a single machine
 * using a local maven repository. No artifacts are deployed remotely, and no
 * results are published to the CI webserver.</p>
 */
public class RegressionTestStep extends Builder implements SimpleBuildStep {

  private String pullNumber;
  private String pullBuildNumber;
  private String filter;
  private Double sample;

  @DataBoundConstructor
  public RegressionTestStep(String pullNumber, String pullBuildNumber, String filter, Double sample) {
    this.pullNumber = pullNumber;
    this.pullBuildNumber = pullBuildNumber;
    this.filter = filter;
    this.sample = sample;
  }

  public String getPullNumber() {
    return pullNumber;
  }

  public String getPullBuildNumber() {
    return pullBuildNumber;
  }

  public String getFilter() {
    return filter;
  }

  public Double getSample() {
    return sample;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run,
                      @Nonnull FilePath workspace,
                      @Nonnull Launcher launcher,
                      @Nonnull TaskListener listener) throws InterruptedException, IOException {


    // Expand ${VARIABLES} in the parameters to allow for parameterized plugins

    long pullNumber = Long.parseLong(run.getEnvironment(listener).expand(this.pullNumber));
    long pullBuildNumber = Long.parseLong(run.getEnvironment(listener).expand(this.pullBuildNumber));
    String expandedFilter = run.getEnvironment(listener).expand(filter);

    PullBuildId pullBuildId = new PullBuildId(pullNumber, pullBuildNumber);

    PackageGraph graph = buildPackageGraph(listener, expandedFilter);

    RegressionTestContext testContext = new RegressionTestContext(pullBuildId,
        new WorkerContext(run, listener));

    List<PackageNode> buildOrder = TopologicalSort.sort(graph);
    for (PackageNode node : buildOrder) {
      if(node.isProvided()) {
        listener.getLogger().println(node.getId() + ": Using version " + node.getBuildResult().getBuildVersion());

      } else if(!node.blockingDependencies().isEmpty()) {
        listener.getLogger().println(node.getId() + ": Skipping. Blocked by " + node.blockingDependencies());

      } else {
        RegressionTestBuild build = new RegressionTestBuild(testContext, node);
        build.build();
      }
    }
  }

  private PackageGraph buildPackageGraph(@Nonnull TaskListener listener, String expandedFilter) throws AbortException {
    listener.getLogger().println("Building package graph...");
    PackageGraph graph;
    try {
      graph = new PackageGraphBuilder(listener, true, true)
          .build(expandedFilter, sample);
    } catch (Exception e) {
      throw new AbortException("Failed to build package graph: " + e.getMessage());
    }

    listener.getLogger().printf("Dependency graph built with %d nodes.\n", graph.size());


    return graph;
  }

  private void testsOnly(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener, RenjinVersionId renjinVersion, String expandedFilter) throws IOException, InterruptedException {
    List<PackageVersionId> packages = PackageGraphBuilder.queryList(listener, expandedFilter, sample);

    for (PackageVersionId packageId : packages) {
      RegressionTestRun testRun = new RegressionTestRun(run, listener, renjinVersion, packageId);
      testRun.run();
    }
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<Builder> {
    public DescriptorImpl() {
    }

    public Builder newInstance(StaplerRequest req, JSONObject data) {
      return req.bindJSON(RegressionTestStep.class, data);
    }

    public String getDisplayName() {
      return "Run regression tests on a Renjin Build";
    }
  }
}
