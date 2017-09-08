package org.renjin.ci.jenkins;

import com.google.api.client.repackaged.com.google.common.base.Strings;
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
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.jenkins.graph.PackageGraphBuilder;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * Runs a sample of package tests on a local development build of Renjin.
 */
public class RegressionTestStep extends Builder implements SimpleBuildStep {

  private String filter;
  private Double sample;
  private String renjinVersion;

  @DataBoundConstructor
  public RegressionTestStep(String filter, Double sample, String renjinVersion) {
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

  public String getRenjinVersion() {
    return renjinVersion;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run,
                      @Nonnull FilePath workspace,
                      @Nonnull Launcher launcher,
                      @Nonnull TaskListener listener) throws InterruptedException, IOException {

    RenjinVersionId renjinVersion;
    if (Strings.isNullOrEmpty(this.renjinVersion) || this.renjinVersion.equals("LATEST")) {
      renjinVersion = RenjinCiClient.getLatestRenjinRelease();
    } else {
      renjinVersion = RenjinVersionId.valueOf(run.getEnvironment(listener).expand(this.renjinVersion));
    }

    // Expand ${VARIABLES} in the filter parameter to allow for parameterized plugins
    String expandedFilter = run.getEnvironment(listener).expand(filter);

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
