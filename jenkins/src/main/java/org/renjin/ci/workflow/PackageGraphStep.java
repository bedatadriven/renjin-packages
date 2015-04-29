package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class PackageGraphStep extends AbstractStepImpl {


  @DataBoundConstructor
  public PackageGraphStep() {
  }
  

  @Override
  public StepDescriptor getDescriptor() {
    return super.getDescriptor();
  }
  
  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(PackageGraphExecution.class);
    }

    @Override
    public String getFunctionName() {
      return "packageGraph";
    }

    @Override
    public String getDisplayName() {
      return "Construct a graph of package dependencies";
    }
  }
  
}
