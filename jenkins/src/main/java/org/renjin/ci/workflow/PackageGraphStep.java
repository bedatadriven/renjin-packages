package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class PackageGraphStep extends AbstractStepImpl {

  private String filter;
  private Double sample;

  @DataBoundConstructor
  public PackageGraphStep() {
  }

  public String getFilter() {
    return filter;
  }

  @DataBoundSetter
  public void setFilter(String filter) {
    this.filter = filter;
  }

  public Double getSample() {
    return sample;
  }

  @DataBoundSetter
  public void setSample(Double sample) {
    this.sample = sample;
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
