package org.renjin.cran;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.renjin.repo.model.BuildOutcome;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

/**
 * Builds a specific commit of Renjin itself and installs it
 * to a commit-specific local maven repository.
 *
 * This allows us to build and test packages agains this
 * specific commit.
 */
public class RenjinBuilder {

  private static final int TIMEOUT_SECONDS = 30 * 60;
  private final Workspace workspace;
  private final File logFile;


  public RenjinBuilder(Workspace workspace) {
    this.workspace = workspace;
    this.logFile = new File(workspace.getRenjinDir(), "build.log");
  }

  public BuildOutcome call() throws Exception {

    BuildOutcome outcome = BuildOutcome.ERROR;

    List<String> command = Lists.newArrayList();
    command.add("mvn");
    command.add("-X");
    command.add("-DenvClassifier=linux-x86_64");
    command.add("-Dmaven.repo.local=" + workspace.getLocalMavenRepository().getAbsolutePath());
    command.add("clean");
    command.add("install");

    ProcessBuilder builder = new ProcessBuilder(command);

    builder.directory(workspace.getRenjinDir());
    builder.redirectErrorStream(true);
    try {
      long startTime = System.currentTimeMillis();
      Process process = builder.start();

      InputStream processOutput = process.getInputStream();
      OutputCollector collector = new OutputCollector(processOutput, logFile);
      collector.setName("Renjin build - output collector");
      collector.start();

      try {
        ProcessMonitor monitor = new ProcessMonitor(process);
        monitor.setName("Renjin build monitor");
        monitor.start();

        while(!monitor.isFinished()) {

          if(System.currentTimeMillis() > (startTime + TIMEOUT_SECONDS * 1000)) {
            System.out.println("Renjin build timed out after " + TIMEOUT_SECONDS + " seconds.");
            process.destroy();
            outcome = BuildOutcome.TIMEOUT;
            break;
          }
          Thread.sleep(1000);
        }

        collector.join();
        if(outcome != BuildOutcome.TIMEOUT) {
          if(monitor.getExitCode() == 0) {
            outcome = BuildOutcome.SUCCESS;
          } else if(monitor.getExitCode() == 1) {
            outcome = BuildOutcome.FAILED;
          } else {
            System.out.println("Renjin build exited with code " + monitor.getExitCode());
            outcome = BuildOutcome.ERROR;
          }
        }
      } finally {
        Closeables.closeQuietly(processOutput);
      }

    } catch (Exception e) {
      outcome = BuildOutcome.ERROR;
      e.printStackTrace();
    }


    workspace.setRenjinBuildOutcome(outcome);

    return outcome;
  }
}
