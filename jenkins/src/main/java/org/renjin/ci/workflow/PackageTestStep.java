package org.renjin.ci.workflow;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Tests a package by running its examples
 */
public class PackageTestStep extends AbstractStepImpl {

  private String renjinVersion;
  private String packageVersionId;

  @DataBoundConstructor
  public PackageTestStep() {
  }

  @DataBoundSetter
  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  @DataBoundSetter
  public void setPackageVersionId(String packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(PackageTestExecution.class);
    }

    @Override
    public String getFunctionName() {
      return "testPackage";
    }

    @Override
    public String getDisplayName() {
      return "Test a package";
    }
  }

}
