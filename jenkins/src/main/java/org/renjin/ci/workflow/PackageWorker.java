package org.renjin.ci.workflow;

import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by alex on 28-4-15.
 */
public class PackageWorker extends BodyExecution {
  @Override
  public Collection<StepExecution> getCurrentExecutions() {
    return null;
  }

  @Override
  public boolean cancel(CauseOfInterruption... causes) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public Object get() throws InterruptedException, ExecutionException {
    return null;
  }

  @Override
  public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return null;
  }
}
