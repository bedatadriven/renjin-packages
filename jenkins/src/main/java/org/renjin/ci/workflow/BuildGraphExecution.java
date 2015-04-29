package org.renjin.ci.workflow;


import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.workflow.queue.PackageGraph;
import org.renjin.ci.workflow.queue.PackageNode;

import javax.inject.Inject;

public class BuildGraphExecution extends StepExecution {

  @Inject
  private transient BuildPackageStep step;
  
  @Inject
  private transient StepContext context;
  
  @StepContextParameter
  private transient TaskListener taskListener;
  
  @StepContextParameter
  private transient FlowNode flowNode;
  
  @Override
  public boolean start() throws Exception {

    // First build the graph
    taskListener.getLogger().println("Constructing package graph...");
    PackageGraph graph = new PackageGraph(taskListener);
    graph.add(PackageVersionId.fromTriplet(step.getPackageVersionId()));
    
    // build the first task
    PackageNode node = graph.take();
    taskListener.getLogger().println("First package: " + node);
    
    context.newBodyInvoker();
    
    
    context.setResult(null);
    
    
    
    return true;
  }

  @Override
  public void stop(Throwable cause) throws Exception {

  }
}
