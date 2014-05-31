package org.renjin.build.agent.build;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.renjin.build.agent.util.OutputCollector;
import org.renjin.build.agent.util.ProcessMonitor;
import org.renjin.build.agent.workspace.Workspace;
import org.renjin.build.model.BuildOutcome;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds a specific commit of Renjin itself and installs it
 * to a commit-specific local maven repository.
 *
 * This allows us to build and test packages against this
 * specific commit.
 */
public class RenjinBuilder {

  private static final int TIMEOUT_MINUTES = 30;
  private final Workspace workspace;
  private final File logFile;


  public RenjinBuilder(Workspace workspace) {
    this.workspace = workspace;
    this.logFile = new File(workspace.getRenjinDir(), "build.log");
  }

  public BuildOutcome call() throws Exception {


    // build!

    BuildOutcome outcome = BuildOutcome.ERROR;

    List<String> command = Lists.newArrayList();
    command.add("mvn");
    command.add("-DenvClassifier=linux-x86_64");
    command.add("-Dmaven.repo.local=" + workspace.getLocalMavenRepository().getAbsolutePath());
    command.add("clean");
    command.add("install");

    ProcessBuilder builder = new ProcessBuilder(command);

    builder.directory(workspace.getRenjinDir());
    builder.redirectErrorStream(true);

    Process process = builder.start();

    OutputCollector collector = new OutputCollector(process, logFile, Integer.MAX_VALUE);
    collector.setName("Renjin build - output collector");
    collector.start();

    try {
      ProcessMonitor monitor = new ProcessMonitor(process);
      monitor.setName("Renjin build monitor");
      monitor.start();

      Stopwatch stopwatch = Stopwatch.createStarted();

      while(!monitor.isFinished()) {

        System.out.println(stopwatch);

        if(stopwatch.elapsed(TimeUnit.MINUTES) > TIMEOUT_MINUTES) {
          System.out.println("Renjin build timed out after " + TIMEOUT_MINUTES + " minutes.");
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
    } catch (Exception e) {
      outcome = BuildOutcome.ERROR;
      e.printStackTrace();
    }

    workspace.setRenjinBuildOutcome(outcome);
    return outcome;
  }
}
