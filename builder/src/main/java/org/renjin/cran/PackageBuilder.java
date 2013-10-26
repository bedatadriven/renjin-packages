package org.renjin.cran;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.renjin.repo.model.BuildOutcome;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callable (concurrent) task that builds a package.
 */
public class PackageBuilder implements Callable<BuildResult> {

  private static final Logger LOGGER = Logger.getLogger(PackageBuilder.class.getName());
  private final Workspace workspace;
  private final BuildReporter reporter;

  private PackageNode pkg;
  private File logFile;

  public static final long TIMEOUT_SECONDS = 20 * 60;

  public PackageBuilder(Workspace workspace, BuildReporter reporter, PackageNode pkg) {
    this.workspace = workspace;
    this.reporter = reporter;
    this.pkg = pkg;
    this.logFile = new File(pkg.getBaseDir(), "build.log");
  }

  @Override
  public BuildResult call() throws Exception {
   
    // set the name of this thread to the package
    // name for debugging
    Thread.currentThread().setName(pkg.getName());

    // write out the POM file for this package
    PomBuilder pomBuilder = new PomBuilder(workspace.getRenjinVersion(), pkg.getBaseDir());
    pomBuilder.writePom();

    BuildResult result = new BuildResult();
    result.setPackageVersionId(pkg.getName());

    List<String> command = Lists.newArrayList();
    command.add(getMavenPath());
    command.add("-X");

    // for snapshots,
    // configure maven to use ONLY our local repo to which we deployed
    // our specific versions of Renjin that we're testing against
    if(workspace.isSnapshot()) {
      command.add("-o");
      command.add("-Dmaven.repo.local=" + workspace.getLocalMavenRepository().getAbsolutePath());
    }

    command.add("-DenvClassifier=linux-x86_64");
    command.add("-Dignore.gnur.compilation.failure=true");
    command.add("-Dmaven.test.failure.ignore=true");

    command.add("clean");
    command.add("install");

    ProcessBuilder builder = new ProcessBuilder(command);
    
    builder.directory(pkg.getBaseDir());
    builder.redirectErrorStream(true);
    try {
      long startTime = System.currentTimeMillis();
      Process process = builder.start();

      InputStream processOutput = process.getInputStream();
      OutputCollector collector = new OutputCollector(processOutput, logFile);
      collector.setName(pkg + " - output collector");
      collector.start();

      try {
        ProcessMonitor monitor = new ProcessMonitor(process);
        monitor.setName(pkg + " - monitor");
        monitor.start();

        while(!monitor.isFinished()) {

          if(System.currentTimeMillis() > (startTime + TIMEOUT_SECONDS * 1000)) {
            System.out.println(pkg + " build timed out after " + TIMEOUT_SECONDS + " seconds.");
            process.destroy();
            result.setOutcome(BuildOutcome.TIMEOUT);
            break;
          }
          Thread.sleep(1000);
        }

        collector.join();
        if(result.getOutcome() != BuildOutcome.TIMEOUT) {
          if(monitor.getExitCode() == 0) {
            result.setOutcome(BuildOutcome.SUCCESS);
          } else if(monitor.getExitCode() == 1) {
            result.setOutcome(BuildOutcome.FAILED);
          } else {
            System.out.println(pkg.getName() + " exited with code " + monitor.getExitCode());
            result.setOutcome(BuildOutcome.ERROR);
          }
        }
      } finally {
        Closeables.closeQuietly(processOutput);
      }
      
    } catch (Exception e) {
      result.setOutcome(BuildOutcome.ERROR);
      e.printStackTrace();
    }

    try {
      reporter.reportResult(pkg, result.getOutcome());
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception recording build results for " + pkg.getPackageVersionId(), e);
    }

    return result; 
  }

  private String getMavenPath() {
    if(System.getProperty("os.name").toLowerCase().contains("windows")) {
      return "mvn.bat";
    } else {
      return "mvn";
    }
  }
}
