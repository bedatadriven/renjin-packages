package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.workflow.graph.BuildQueue;

/**
 * Downloads package sources from Google Cloud Storage and
 * extracts them into the workspace.
 */
public class BuildPackageStep extends AbstractStepImpl {

  private String renjinVersion;
  private BuildQueue.Lease lease;

  @DataBoundConstructor
  public BuildPackageStep(String renjinVersion, BuildQueue.Lease lease) {
    this.renjinVersion = renjinVersion;
    this.lease = lease;
  }

  public BuildQueue.Lease getLeasedBuild() {
    return lease;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  @Override
  public StepDescriptor getDescriptor() {
    return super.getDescriptor();
  }

  public PackageVersionId getPackageVersionId() {
    return lease.getPackageVersionId();
  }

  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(BuildPackageExecution.class);
    }
    

    @Override
    public String getFunctionName() {
      return "buildPackage";
    }

    @Override
    public String getDisplayName() {
      return "Build Renjin Package";
    }
  }

}
