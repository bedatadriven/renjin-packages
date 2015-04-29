package org.renjin.ci.workflow;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.renjin.ci.workflow.graph.PackageGraph;

import javax.inject.Inject;


public class PackageGraphExecution extends AbstractSynchronousStepExecution<PackageGraph> {
  
  @Override
  protected PackageGraph run() throws Exception {
    return new PackageGraph();
  }
}
