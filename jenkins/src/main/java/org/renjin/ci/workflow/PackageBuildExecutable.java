package org.renjin.ci.workflow;

import com.google.common.collect.Iterables;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.SubTask;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.workflow.graph.PackageNode;
import org.renjin.ci.workflow.tools.GoogleCloudStorage;
import org.renjin.ci.workflow.tools.LogFileParser;
import org.renjin.ci.workflow.tools.Maven;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class PackageBuildExecutable implements Queue.Executable {
  
  private Run<?, ?> parentRun;
  private TaskListener listener;
  private PackageBuildTask parentTask;
  

  public PackageBuildExecutable(Run<?, ?> parentRun, TaskListener listener, PackageBuildTask parentTask) {
    this.parentRun = parentRun;
    this.listener = listener;
    this.parentTask = parentTask;
  }

  @Nonnull
  @Override
  public SubTask getParent() {
    return parentTask;
  }

  @Override
  public long getEstimatedDuration() {
    // Ballpark "typical" time required for package build
    return TimeUnit.SECONDS.toMillis(30);
  }
  
  @Override
  public void run() {
    
    List<String> blockingDependencies = parentTask.getPackageNode().blockingDependencies();
    if(blockingDependencies.isEmpty()) {
      build();
    } else {
      reportBlocked(blockingDependencies);
    }
  }

  private void reportBlocked(List<String> blockingDependencies) {

    
    PackageNode node = parentTask.getPackageNode();

    listener.getLogger().println(format("%s: Blocked by failed dependencies %s", node.getId(),
        Iterables.toString(blockingDependencies)));


    // Mark the node as failed.
    
    // Report the failure immediately, we can't proceed with the build
    PackageBuild packageBuild = null;
    try {
      packageBuild = RenjinCiClient.startBuild(node.getId(), parentTask.getRenjinVersion());
    } catch (Exception e) {
      throw new RuntimeException("Could not post new build.", e);
    }

    node.blocked(packageBuild.getBuildNumber());

    PackageBuildResult result = new PackageBuildResult(BuildOutcome.BLOCKED);
    result.setId(packageBuild.getId().toString());
    result.setBlockingDependencies(blockingDependencies);
    result.setResolvedDependencies(node.resolvedDependencies());
    RenjinCiClient.postResult(packageBuild, result);
    
  }


  private void build() {
    PackageVersionId pvid = parentTask.getPackageVersionId();

    try {
      WorkerContext workerContext = new WorkerContext(parentRun, listener);
      Maven maven = new Maven(workerContext);

      BuildContext buildContext = new BuildContext(workerContext, maven, parentTask.getPackageNode());
  
      /*
       * Download and unpack the original source of this package
       */
      GoogleCloudStorage.downloadAndUnpackSources(buildContext, pvid);

      PackageBuildResult result;

      PackageBuild build = RenjinCiClient.startBuild(pvid, parentTask.getRenjinVersion());

      buildContext.log("Starting build #%d...", build.getBuildNumber());

      maven.writePom(buildContext, build);

      maven.build(buildContext);

      /**
       * Parse the result of the build from the log files
       */
      result = LogFileParser.parse(buildContext);
      result.setResolvedDependencies(buildContext.getPackageNode().resolvedDependencies());
      
      /**
       * Archive the build log file permanently to Google Cloud Storage
       */
      GoogleCloudStorage.archiveLogFile(buildContext, build);

      /**
       * Report the build result to ci.renjin.org
       */
      RenjinCiClient.postResult(build, result);

      listener.getLogger().println(pvid + ": " + result.getOutcome());

      parentTask.getPackageNode().completed(build.getBuildNumber(), result.getOutcome());

    } catch (InterruptedException e) {
      parentTask.getPackageNode().cancelled();
      throw new RuntimeException("Cancelled", e);

    } catch (Exception e) {
      listener.getLogger().printf("Exception building: %s\n", e.getMessage());
      parentTask.getPackageNode().crashed();
      e.printStackTrace(listener.getLogger());
    }
  }

}
