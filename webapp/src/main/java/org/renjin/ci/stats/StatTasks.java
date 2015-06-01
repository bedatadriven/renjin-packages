package org.renjin.ci.stats;

import com.google.appengine.api.taskqueue.*;
import com.google.appengine.tools.pipeline.Job;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import org.renjin.ci.datastore.LastEventTime;

import javax.ws.rs.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Path("tasks/stats")
public class StatTasks {

  public static final int QUIET_PERIOD_MILLIS = (int)TimeUnit.MINUTES.toMillis(5);

  private static final Logger LOGGER = Logger.getLogger(StatTasks.class.getName());

  /**
   * Schedules a map-reduce job to update the package build deltas counts, with a quiet period 
   * of {@link org.renjin.ci.stats.StatTasks#QUIET_PERIOD_MILLIS} milliseconds.
   *
   * <p>If another update is scheduled before 
   */
  public static void scheduleBuildDeltaCountUpdate() {
    //triggerUpdate("buildDeltas", StatPipelines.updateBuildStats());
  }

  private static void triggerUpdate(final String taskName, final Job<?> job) {

    // Mark the time of this event. If our deferred task runs and there has been a subsequent
    // event within the time period, the update will be further delayed.

    LastEventTime.update(taskName);

    // Schedule a check after the quiet period to run the update job, if there
    // hasn't been an update in the mean time.
    Queue queue = QueueFactory.getDefaultQueue();
    TaskHandle taskHandle = queue.add(TaskOptions.Builder
        .withCountdownMillis(QUIET_PERIOD_MILLIS)
        .payload(new DeferredTask() {
          @Override
          public void run() {

            if (LastEventTime.getMillisSinceLastEvent(taskName) > QUIET_PERIOD_MILLIS) {
              LOGGER.info("The quiet period has been quiet. Starting update...");

              PipelineService pipelineService = PipelineServiceFactory.newPipelineService();
              pipelineService.startNewPipelineUnchecked(job, new Object[0]);

            } else {
              LOGGER.info("There has been a new update during the quiet period, postponing update...");
            }
          }
        }));
    
    LOGGER.info("Scheduled deferred task " + taskHandle);
  }

}
