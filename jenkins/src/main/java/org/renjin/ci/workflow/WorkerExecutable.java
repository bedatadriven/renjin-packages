package org.renjin.ci.workflow;

import com.google.common.base.Optional;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.SubTask;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.workflow.graph.BuildQueue;
import org.renjin.ci.workflow.tools.GoogleCloudStorage;
import org.renjin.ci.workflow.tools.LogFileParser;
import org.renjin.ci.workflow.tools.Maven;
import org.renjin.ci.workflow.tools.RenjinCiClient;

import javax.annotation.Nonnull;
import java.io.IOException;


public class WorkerExecutable implements Queue.Executable {
  
  private final WorkerTask parent;
  private final TaskListener taskListener;


  public WorkerExecutable(WorkerTask parent, TaskListener taskListener) {
    this.parent = parent;
    this.taskListener = taskListener;
  }

  @Nonnull
  @Override
  public SubTask getParent() {
    return parent;
  }

  @Override
  public void run() {

    WorkerContext workerContext = null;
    Maven maven;
    try {
      workerContext = new WorkerContext(parent.getRun(), taskListener);
      maven = new Maven(workerContext);

    } catch (InterruptedException e) {
      // Cancelled
      return;
    } catch (IOException e) {
      taskListener.getLogger().println("Failed to create worker context");
      e.printStackTrace(taskListener.getLogger());
      return;
    }
    
    
    while(true) {
      Optional<BuildQueue.Lease> lease = parent.getBuildQueue().take();
      if(!lease.isPresent()) {
        // end of queue
        break;
      }

      try {
        buildNext(workerContext, maven, lease.get());

      } catch (Exception e) {
        // something's wrong with the executor/jenkins/node, 
        // exit now, and we'll be replaced by PackageGraphExecution if there is more work to be done
        return;
      }
    }
  }

  private void buildNext(WorkerContext workerContext, Maven maven, BuildQueue.Lease lease) throws InterruptedException, IOException {
    

    BuildContext buildContext = new BuildContext(workerContext, maven, lease.getNode());

    /*
     * Download and unpack the original source of this package
     */
    GoogleCloudStorage.downloadAndUnpackSources(buildContext, lease.getPackageVersionId());
    
    PackageBuildResult result;
    
    try {
      
      PackageBuild build = RenjinCiClient.startBuild(lease.getPackageVersionId(), parent.getRenjinVersion());

      buildContext.log("Starting build #%d...", build.getBuildNumber());
      
      maven.writePom(buildContext, build);
      
      maven.build(buildContext);

      /**
       * Parse the result of the build from the log files
       */
      result = LogFileParser.parse(buildContext);

      /**
       * Archive the build log file permanently to Google Cloud Storage
       */
      GoogleCloudStorage.archiveLogFile(buildContext, build);

      /**
       * Report the build result to ci.renjin.org
       */
      RenjinCiClient.postResult(build, result);
      
      lease.completed(taskListener, build.getBuildNumber(), result.getOutcome());
      
      
    } catch (InterruptedException e) {
      throw new InterruptedException();  
    } catch (Exception e) {
      buildContext.log("Exception building: %s", e.getMessage());
      e.printStackTrace(buildContext.getLogger());
      lease.crashed();
    }
    
  }

  @Override
  public long getEstimatedDuration() {
    return -1;
  }
}
