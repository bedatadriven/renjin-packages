package org.renjin.ci.worker;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.task.PackageBuildResult;
import org.renjin.ci.task.PackageBuildTask;
import org.renjin.ci.model.BuildOutcome;

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

  private File logFile;

  public static final long TIMEOUT_MINUTES = 20;

  public static final int MAX_LOG_SIZE = 1024 * 600;

  private NativeOutcome nativeOutcome = NativeOutcome.NA;

  public PackageBuilder(PackageBuildTask build) {
    this.build = build;
    this.baseDir = Files.createTempDir();
    this.logFile = new File(baseDir, "build.log");

    LOGGER.info("Building " + build + " in " + baseDir);
  }

  public void build()  {

    LOGGER.info("Building " + build);
   
    // set the name of this thread to the package
    // name for debugging
    Thread.currentThread().setName(build.toString());

    try {
      unpackSources();
      executeMaven();
      publishBuildLog();
      removeBuildDir();

    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Exception building " + build + ": " + e.getMessage());
      e.printStackTrace();
      outcome = BuildOutcome.ERROR;
    }
  }


  /**
   * Check whether the source is already unpacked in our workspace,.
   * otherwise download and unpack
   */
  private void unpackSources() throws IOException {

    StorageClient storage = new StorageClient();

    try(InputStream in = storage.openSourceArchive(build.getPackageVersionId())) {

      TarArchiveInputStream tarIn = new TarArchiveInputStream(
          new GZIPInputStream(in));

      TarArchiveEntry entry;
      while((entry=tarIn.getNextTarEntry())!=null) {
        if(entry.isFile()) {

          String name = stripPackageDir(entry.getName());

          File outFile = new File(baseDir.getAbsolutePath() + File.separator + name);
          Files.createParentDirs(outFile);

          Files.asByteSink(outFile).writeFrom(tarIn);
        }
      }
    }
  }

  private String stripPackageDir(String name) {
    int slash = name.indexOf('/');
    return name.substring(slash+1);
  }

  private void executeMaven() throws IOException, InterruptedException {

    LOGGER.info("Writing pom.xml");
    Files.write(build.getPom().getBytes(Charsets.UTF_8), new File(baseDir, "pom.xml"));

    LOGGER.info("Executing maven...");


    List<String> command = Lists.newArrayList();
    command.add("mvn");
    command.add("-e"); // show full stack traces

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
    collector.setName(build + " - output collector");
    collector.start();

    ProcessMonitor monitor = new ProcessMonitor(process);
    monitor.setName(build + " - monitor");
    monitor.start();

    Stopwatch stopwatch = Stopwatch.createStarted();

    while(!monitor.isFinished()) {

      if(stopwatch.elapsed(TimeUnit.MINUTES) > TIMEOUT_MINUTES) {
        System.out.println(build + " build timed out after " + TIMEOUT_MINUTES + " minutes.");
        process.destroy();
        outcome = BuildOutcome.TIMEOUT;
        break;
      }
      Thread.sleep(1000);
    }

    collector.join();

    nativeOutcome = collector.getNativeOutcome();

    if(outcome != BuildOutcome.TIMEOUT) {
      if(monitor.getExitCode() == 0) {
        outcome = BuildOutcome.SUCCESS;
      } else if(monitor.getExitCode() == 1) {
        outcome = BuildOutcome.FAILURE;
      } else {
        System.out.println(build + " exited with code " + monitor.getExitCode());
        outcome = BuildOutcome.ERROR;
      }
    }
  }

  private void publishBuildLog() {
    try {
      StorageClient.INSTANCE.putBuildLog(
          build.getBuildNumber(),
          build.getPackageVersionId(),
          Files.asByteSource(logFile));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to publish build log", e);
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
    result.setId(build.toString());
    result.setOutcome(outcome);
    result.setNativeOutcome(nativeOutcome);
    return result;
  }
}
