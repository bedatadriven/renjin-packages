package org.renjin.build.worker;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.task.PackageBuildResult;
import org.renjin.build.task.PackageBuildTask;
import org.renjin.build.worker.util.GoogleCloudStorage;
import org.renjin.build.worker.util.OutputCollector;
import org.renjin.build.worker.util.ProcessMonitor;
import org.renjin.build.model.BuildOutcome;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;


/**
 * Callable (concurrent) task that builds a package.
 */
public class PackageBuilder {

  private static final Logger LOGGER = Logger.getLogger(PackageBuilder.class.getName());

  private final File baseDir;
  private final PackageBuildTask build;

  private BuildOutcome outcome = BuildOutcome.ERROR;
  private boolean nativeSourceCompilationFailures = false;

  private File logFile;

  public static final long TIMEOUT_MINUTES = 20;

  public static final int MAX_LOG_SIZE = 1024 * 600;

  public PackageBuilder(File packagesDir, PackageBuildTask build) {
    this.build = build;
    this.baseDir = new File(packagesDir, build.getPackageName() + "_" + build.getPackageVersion());
    this.logFile = new File(baseDir, "build.log");
  }

  public void build()  {

    System.out.println("Building " + build.packageBuildId());
   
    // set the name of this thread to the package
    // name for debugging
    Thread.currentThread().setName(build.getPackageName());

    try {
      // grab the source if we don't have it
      ensureSourceUnpacked();

      // write out the POM file for this package
      PomBuilder pomBuilder = new PomBuilder(baseDir, build);
      pomBuilder.writePom();

      executeMaven();

      publishBuildLog();

      removeBuildDir();

    } catch(Exception e) {
      System.out.println("Exception building " + build.packageBuildId() + ": " + e.getMessage());
      e.printStackTrace();
      outcome = BuildOutcome.ERROR;
    }
  }


  private void publishBuildLog() {
    try {
      GoogleCloudStorage.INSTANCE.putBuildLog(
          build.getBuildId(),
          build.versionId(),
          Files.asByteSource(logFile));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to publish build log", e);
    }
  }


  /**
   * Check whether the source is already unpacked in our workspace,
   * otherwise download and unpack
   */
  private void ensureSourceUnpacked() throws IOException {

    if(!baseDir.exists()) {

      InputStream in = GoogleCloudStorage.INSTANCE.openSourceArchive(
          build.getPackageGroupId(),
          build.getPackageName(),
          build.getPackageVersion());

      TarArchiveInputStream tarIn = new TarArchiveInputStream(
          new GZIPInputStream(in));

      String packagePrefix = build.getPackageName() + "/";

      TarArchiveEntry entry;
      while((entry=tarIn.getNextTarEntry())!=null) {
        if(entry.isFile() && entry.getName().startsWith(packagePrefix)) {

          String name = entry.getName().substring(packagePrefix.length());

          File outFile = new File(baseDir.getAbsolutePath() + File.separator + name);
          outFile.getParentFile().mkdirs();

          Files.asByteSink(outFile).writeFrom(tarIn);
        }
      }
      tarIn.close();
    }
  }

  private void executeMaven() throws IOException, InterruptedException {

    System.out.println("Starting maven...");

    List<String> command = Lists.newArrayList();
    command.add("mvn");
    command.add("-X");
//    command.add("--gs");
//    command.add("/etc/renjin-worker/settings.xml");

    command.add("-DenvClassifier=linux-x86_64");
    command.add("-Dignore.gnur.compilation.failure=true");

    command.add("-DskipTests");
    command.add("-B"); // run in batch mode

    command.add("clean");
    command.add("deploy");

    ProcessBuilder builder = new ProcessBuilder(command);

    builder.directory(baseDir);
    builder.redirectErrorStream(true);

    Process process = builder.start();

    OutputCollector collector = new OutputCollector(process, logFile, MAX_LOG_SIZE);
    collector.setName(build.getPackageName() + " - output collector");
    collector.start();

    ProcessMonitor monitor = new ProcessMonitor(process);
    monitor.setName(build.packageBuildId() + " - monitor");
    monitor.start();

    Stopwatch stopwatch = Stopwatch.createStarted();

    while(!monitor.isFinished()) {

      if(stopwatch.elapsed(TimeUnit.MINUTES) > TIMEOUT_MINUTES) {
        System.out.println(build.packageBuildId() + " build timed out after " + TIMEOUT_MINUTES + " minutes.");
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
        outcome = BuildOutcome.FAILURE;
      } else {
        System.out.println(build.packageBuildId() + " exited with code " + monitor.getExitCode());
        outcome = BuildOutcome.ERROR;
      }
    }
  }


  private void removeBuildDir() {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"rm","-rf", baseDir.getAbsolutePath() });
      p.waitFor();
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Failed to remove build dir", e);
    }
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public PackageBuildResult getResult() {
    PackageBuildResult result = new PackageBuildResult();
    result.setId(build.packageBuildId());
    result.setOutcome(outcome);
    result.setNativeSourcesCompilationFailure(nativeSourceCompilationFailures);
    return result;
  }
}
