package org.renjin.ci.workflow;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import jenkins.util.Timer;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.workflow.graph.*;

import javax.inject.Inject;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


public class PackageGraphExecution extends AbstractStepExecutionImpl {
  
  @Inject
  private transient PackageGraphStep step;

  @StepContextParameter
  private transient TaskListener taskListener;

  @StepContextParameter
  private transient Run<?, ?> run;
  
  
  private PackageGraph graph;

  /**
   * Our internal queue of packages to build
   */
  private transient BuildQueue buildQueue;

  /**
   * Workers running as seperate tasks to handle build tasks
   */
  private transient List<QueueTaskFuture<Queue.Executable>> workers;
  
  private transient ScheduledFuture<?> timer;
  
  private transient int nextWorkerId;


  @Override
  public boolean start() throws Exception {
    
    if(Strings.isNullOrEmpty(step.getRenjinVersion())) {
      getContext().onFailure(new AbortException("renjinVersion must be specified"));
      return true;
    }

    graph = new PackageGraphBuilder(taskListener)
        .add(step.getFilter(), step.getFilterParameters(), step.getSample());
    
    
    // Save the state of our internal queue
    getContext().saveState();
    
    startWorkers();

    return false;
  }

  @Override
  public void onResume() {
    super.onResume();

    startWorkers();
  }

  private void startWorkers() {

    buildQueue = graph.newBuildQueue();
    
    taskListener.getLogger().println(format("Build queue initialized with %d packages to build.", 
        buildQueue.getTotalLength()));
    
    workers = Lists.newArrayList();
    nextWorkerId = 1;

    timer = Timer.get().scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          checkWorkerHealth();
        } catch (InterruptedException e) {
          taskListener.getLogger().println("Interrupted...");
        } catch (Exception e) {
          e.printStackTrace(taskListener.getLogger());
        }
      }
    }, 1, 15, TimeUnit.SECONDS);
  }

  private void checkWorkerHealth() throws InterruptedException {
    taskListener.getLogger().println(format("%d Packages remaining in queue, %d workers active...",
        buildQueue.getTotalLength(), workers.size()));
    
    // Have any of our workers crashed or completed?
    ListIterator<QueueTaskFuture<Queue.Executable>> it = workers.listIterator();
    while(it.hasNext()) {
      QueueTaskFuture<Queue.Executable> worker = it.next();
      if(worker.isDone()) {
        it.remove();
      }
    }
    
    int workerCount = 2;
    if(step.getWorkerCount() != null) {
      workerCount = step.getWorkerCount();
    }
    
    // Now enqueue additional tasks to meet our concurrency requirement
    while(workers.size() < workerCount && !buildQueue.isEmpty()) {
      WorkerTask newWorker = new WorkerTask(nextWorkerId++, run, taskListener, buildQueue, step.getRenjinVersion());
      taskListener.getLogger().println("Starting new worker #" + newWorker.getId() + "...");

      Queue.WaitingItem item = Queue.getInstance().schedule2(newWorker, 0).getCreateItem();
      if(item == null) {
        throw new IllegalStateException("Failed to enqueue " + newWorker);
      }
      workers.add(item.getFuture());
    }
    
    if(buildQueue.isEmpty() && workers.isEmpty()) {
      finish();
    }
    
    // Save our progress so far...
    getContext().saveState();
  }
  
  private void finish() {
    timer.cancel(true);
    
    summarizeResults();
    
    getContext().onSuccess(null);
  }

  private void summarizeResults() {
    
    int succeeded = 0;
    int failed = 0;
    int orphaned = 0;

    for (PackageNode packageNode : graph.getNodes()) {
      taskListener.getLogger().println(packageNode.getId() + ": " + packageNode.getDebugLabel());
      if (!packageNode.isProvided()) {
        if (packageNode.getState() == NodeState.BUILT) {
          if (packageNode.getBuildOutcome() == BuildOutcome.SUCCESS) {
            succeeded++;
          } else {
            failed++;
          }
        } else if (packageNode.getState() == NodeState.ORPHANED) {
          orphaned++;
        }
      }
    }
    
    taskListener.getLogger().println(format("Build complete: %d succeeded, %d failed, %d orphaned.", 
        succeeded, failed, orphaned));
    
    
  }

  @Override
  public void stop(Throwable cause) throws Exception {
    
    summarizeResults();
    
    // cancel
    if(timer != null) {
      timer.cancel(false);
    }
    
    // cancel any pending tasks
    if(workers != null) {
      for (QueueTaskFuture<Queue.Executable> worker : workers) {
        worker.cancel(true);
      }
    }

    getContext().onFailure(cause);
  }
}
