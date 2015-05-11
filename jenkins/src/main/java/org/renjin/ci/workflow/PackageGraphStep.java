package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Map;

public class PackageGraphStep extends AbstractStepImpl {

  private String filter;
  private Map filterParameters;
  private Double sample;
  private Integer workerCount;
  private String renjinVersion;
  
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

  public Map getFilterParameters() {
    return filterParameters;
  }

  @DataBoundSetter
  public void setFilterParameters(Map filterParameters) {
    this.filterParameters = filterParameters;
  }

  @Override
  public StepDescriptor getDescriptor() {
    return super.getDescriptor();
  }

  
  public String getRenjinVersion() {
    return renjinVersion;
  }

  @DataBoundSetter
  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public Integer getWorkerCount() {
    return workerCount;
  }

  @DataBoundSetter
  public void setWorkerCount(Integer workerCount) {
    this.workerCount = workerCount;
  }

  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(PackageGraphExecution.class);
    }

    @Override
    public String getFunctionName() {
      return "buildPackages";
    }

    @Override
    public String getDisplayName() {
      return "Build a set of interdependent packages";
    }
  }
  
}
