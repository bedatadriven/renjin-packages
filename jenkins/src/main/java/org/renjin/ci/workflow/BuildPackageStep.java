package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.renjin.ci.workflow.BuildPackageExecution;

/**
 * Downloads package sources from Google Cloud Storage and
 * extracts them into the workspace.
 */
public class BuildPackageStep extends AbstractStepImpl {

  private String renjinVersion;
  private String packageVersionId;

  @DataBoundConstructor
  public BuildPackageStep(String renjinVersion, String packageVersionId) {
    this.renjinVersion = renjinVersion;
    this.packageVersionId = packageVersionId;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }
  
  public String getRenjinVersion() {
    return renjinVersion;
  }

  @Override
  public StepDescriptor getDescriptor() {
    return super.getDescriptor();
  }
  
  

  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(BuildGraphExecution.class);
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
