package org.renjin.ci.jenkins;

import hudson.model.Queue;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class Tasks {
  static void waitForTasks(TaskListener listener, List<Queue.WaitingItem> queueItems) {
    // Wait for everything to complete!
    try {
      for (Queue.WaitingItem queueItem : queueItems) {
        try {
          queueItem.getFuture().get();
        } catch (ExecutionException e) {
          listener.fatalError(queueItem.task.getName() + " failed: " + e.getMessage());
        }
      }
      listener.getLogger().println("Queue complete.");

    } catch (InterruptedException e) {
      for (Queue.WaitingItem waitingItem : queueItems) {
        if(!waitingItem.getFuture().isDone()) {
          Jenkins.getInstance().getQueue().cancel(waitingItem);
        }
      }      
    }
  }
}
