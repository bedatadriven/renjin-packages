package org.renjin.ci.jenkins;

import com.google.common.collect.Iterables;
import hudson.FilePath;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.SubTask;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.tools.*;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.model.PackageVersionId;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
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
      node.crashed();
      throw new RuntimeException("Could not post new build.", e);
    }

    node.blocked(packageBuild.getBuildNumber());

    PackageBuildResult result = new PackageBuildResult(BuildOutcome.BLOCKED);
    result.setId(packageBuild.getId().toString());
    result.setPackageVersionId(packageBuild.getPackageVersionId());
    result.setBlockingDependencies(blockingDependencies);
    result.setResolvedDependencies(node.resolvedDependencies());
    RenjinCiClient.postResult(packageBuild, result);

  }


  private void build() {
    PackageVersionId pvid = parentTask.getPackageVersionId();

    try {
      WorkerContext workerContext = new WorkerContext(parentRun, listener);
      Maven maven = new Maven(workerContext);

      /*
       * Register a new build with the Renjin CI Server and save the build number.
       */
      PackageBuild build = RenjinCiClient.startBuild(pvid, parentTask.getRenjinVersion());

      BuildContext buildContext = new BuildContext(workerContext, maven, parentTask.getPackageNode(),
          "b" + build.getBuildNumber());

      buildContext.log("Starting build #%d on %s...", build.getBuildNumber(), workerContext.getNode().getDisplayName());

      try {

        PackageBuildResult result;

        /*
         * Download and unpack the original source of this package
         */
        String patchId = null;

        patchId = tryDownloadPatchedVersion(buildContext, pvid);
        if(patchId == null) {
          GoogleCloudStorage.downloadAndUnpackSources(buildContext, pvid);
        }

        // Check that this package has a NAMESPACE. We will not build ancient
        // packages without namespaces anymore.
        FilePath namespaceFile = buildContext.getBuildDir().child("NAMESPACE");
        if(!namespaceFile.exists()) {
          listener.error("We are no longer building packages without NAMESPACEs. Skipping " + pvid);
          return;
        }

        maven.writeReleasePom(buildContext, build);

        // TODO: change to package once we are ready to shutdown nexus
        maven.build(buildContext, "deploy");

        /*
         * Parse the result of the build from the log files
         */
        result = LogFileParser.parse(buildContext);
        result.setPatchId(patchId);
        result.setResolvedDependencies(buildContext.getPackageNode().resolvedDependencies());

        /*
         * Archive the pom and jar
         */
        if(result.getOutcome() == BuildOutcome.SUCCESS) {
          archiveArtifacts(buildContext);
        }

        /*
         * Archive the build log file permanently to Google Cloud Storage
         */
        GcsLogArchiver logArchiver = GoogleCloudStorage.newArchiver(buildContext);

        logArchiver.archiveLog();

        /*
         * Test results
         */
        result.setTestResults(TestResultParser.parseResults(buildContext, logArchiver));

        logArchiver.archivePlots(buildContext.getPlotDir());


        /*
         * Report the build result to ci.renjin.org
         */
        RenjinCiClient.postResult(build, result);

        listener.hyperlink("http://packages.renjin.org" + build.getId().getPath(), pvid + ": " + result.getOutcome());
        listener.getLogger().println();

        parentTask.getPackageNode().completed(build.getBuildNumber(), result.getOutcome());

      } finally {

        /*
         * Make sure we clean up our workspace so we don't fill the builder's storage
         */
        buildContext.cleanup();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      parentTask.getPackageNode().cancelled();
      throw new RuntimeException("Cancelled", e);

    } catch (Exception e) {
      listener.getLogger().printf("Exception building: %s%n", e.getMessage());
      parentTask.getPackageNode().crashed();
      e.printStackTrace(listener.getLogger());
    }
  }

  private void archiveArtifacts(BuildContext buildContext) throws IOException, InterruptedException {

    PackageVersionId pvid = buildContext.getPackageVersionId();
    String buildVersion = pvid.getVersionString() + "-" + buildContext.getBuildNumber();


    FilePath pomFile = buildContext.getBuildDir().child(format("%s-%s.pom",
        pvid.getPackageName(),
        buildVersion));

    buildContext.getBuildDir().child("pom.xml").copyTo(pomFile);

    FilePath jarFile = buildContext.getBuildDir().child(String.format("target/%s-%s.jar",
      pvid.getPackageName(),
      buildVersion));

    List<String> cmd = new ArrayList<>();
    cmd.add("gsutil");
    cmd.add("-m");
    cmd.add("cp");

    cmd.add(pomFile.getRemote());
    cmd.addAll(RenjinCiDeployer.computeDigests(pomFile, buildContext.getListener()));

    cmd.add(jarFile.getRemote());
    cmd.addAll(RenjinCiDeployer.computeDigests(jarFile, buildContext.getListener()));

    cmd.add(String.format("gs://renjinci-artifacts/%s/%s/%s/",
        pvid.getGroupId(),
        pvid.getPackageName(),
        buildVersion));

    buildContext.getWorkerContext().getLauncher().launch().cmds(cmd).start().join();
  }

  private String tryDownloadPatchedVersion(BuildContext buildContext, PackageVersionId pvid) throws IOException, InterruptedException {
    String patchId = RenjinCiClient.getPatchedVersionId(pvid);
    buildContext.log("Found patch " + patchId);
    if(patchId != null) {
      FilePath patchedArchive = buildContext.getWorkerContext().child("patched.zip");
      buildContext.log("Downloading patched archive to " + patchedArchive.getRemote() + "...");
      patchedArchive.copyFrom(RenjinCiClient.getPatchedVersionUrl(pvid));

      FilePath targetDir = buildContext.getWorkerContext().getWorkspace();
      buildContext.log("Unpacking patched sources into " + targetDir.getRemote() + "...");
      patchedArchive.unzip(targetDir);

      buildContext.setBuildDir(targetDir.child(String.format("%s.%s-patched-%s",
          pvid.getGroupId(),
          pvid.getPackageName(),
          pvid.getVersionString())));

    }
    return patchId;
  }

}
