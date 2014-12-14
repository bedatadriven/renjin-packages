package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Downloads package sources from Google Cloud Storage and
 * extracts them into the workspace.
 */
public class BuildPackageStep extends AbstractStepImpl {

  private String renjinVersionId;
  private String packageVersionId;

  @DataBoundConstructor
  public BuildPackageStep(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  @DataBoundSetter
  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public String getRenjinVersionId() {
    return renjinVersionId;
  }

  @DataBoundSetter
  public void setRenjinVersionId(String renjinVersionId) {
    this.renjinVersionId = renjinVersionId;
  }

  @Override
  public StepDescriptor getDescriptor() {
    return super.getDescriptor();
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
