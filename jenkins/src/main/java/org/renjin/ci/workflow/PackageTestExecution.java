package org.renjin.ci.workflow;

import hudson.Launcher;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.remoting.RoleChecker;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.test.TestRunner;

import javax.inject.Inject;


public class PackageTestExecution extends AbstractSynchronousStepExecution<Void> {

  @Inject
  private transient PackageTestStep step;
  
  @StepContextParameter
  private Launcher launcher;

  @Override
  protected Void run() throws Exception {
    return launcher.getChannel().call(new Callable<Void, Exception>() {
      @Override
      public void checkRoles(RoleChecker roleChecker) throws SecurityException {

      }

      @Override
      public Void call() throws Exception {

        TestRunner testRunner = new TestRunner(RenjinVersionId.valueOf(step.getRenjinVersion()));
        testRunner.testPackage(PackageVersionId.fromTriplet(step.getPackageVersionId()));
        
        return null;
      }
    });
  }
}
