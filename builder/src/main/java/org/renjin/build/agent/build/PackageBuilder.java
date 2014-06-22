package org.renjin.build.agent.build;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.agent.util.GoogleCloudStorage;
import org.renjin.build.agent.util.OutputCollector;
import org.renjin.build.agent.util.ProcessMonitor;
import org.renjin.build.agent.workspace.Workspace;
import org.renjin.build.model.BuildOutcome;
import org.renjin.build.model.BuildStage;
import org.renjin.build.model.RPackageBuild;
import org.renjin.build.model.RPackageVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;


/**
 * Callable (concurrent) task that builds a package.
 */
public class PackageBuilder {

  private static final Logger LOGGER = Logger.getLogger(PackageBuilder.class.getName());

  private final File baseDir;
  private final RPackageBuild build;
  private RPackageVersion pkg;

  private File logFile;

  public static final long TIMEOUT_MINUTES = 20;

  public static final int MAX_LOG_SIZE = 1024 * 600;

  public PackageBuilder(Workspace workspace, RPackageBuild build) {
    this.build = build;
    this.pkg = build.getPackageVersion();
    this.baseDir = new File(workspace.getPackagesDir(), pkg.getPackageName() + "_" + pkg.getVersion());
    this.logFile = new File(baseDir, "build.log");
  }

  public void build()  {

    System.out.println("Building " + build.getPackage().getId());
   
    // set the name of this thread to the package
    // name for debugging
    Thread.currentThread().setName(pkg.getPackageName());

    try {
      // grab the source if we don't have it
      ensureSourceUnpacked();

      // write out the POM file for this package
      PomBuilder pomBuilder = new PomBuilder(baseDir, build);
      pomBuilder.writePom();

      executeMaven();

    } catch(Exception e) {
      System.out.println("Exception building " + pkg.getId() + ": " + e.getMessage());
      build.setOutcome(BuildOutcome.ERROR);
    }

    build.setStage(BuildStage.COMPLETED);
    build.setCompletionTime(new Date());
  }

  private void executeMaven() throws IOException, InterruptedException {

    System.out.println("Starting maven...");

    List<String> command = Lists.newArrayList();
    command.add("mvn");
    command.add("-X");

    command.add("-DenvClassifier=linux-x86_64");
    command.add("-Dignore.gnur.compilation.failure=true");

    command.add("-DskipTests");

    command.add("clean");
    command.add("deploy");

    ProcessBuilder builder = new ProcessBuilder(command);

    builder.directory(baseDir);
    builder.redirectErrorStream(true);

    Process process = builder.start();

    OutputCollector collector = new OutputCollector(process, logFile, MAX_LOG_SIZE);
    collector.setName(pkg + " - output collector");
    collector.start();

    ProcessMonitor monitor = new ProcessMonitor(process);
    monitor.setName(pkg.getId() + " - monitor");
    monitor.start();

    Stopwatch stopwatch = Stopwatch.createStarted();

    while(!monitor.isFinished()) {

      if(stopwatch.elapsed(TimeUnit.MINUTES) > TIMEOUT_MINUTES) {
        System.out.println(pkg + " build timed out after " + TIMEOUT_MINUTES + " minutes.");
        process.destroy();
        build.setOutcome(BuildOutcome.TIMEOUT);
        break;
      }
      Thread.sleep(1000);
    }

    collector.join();
    if(build.getOutcome() != BuildOutcome.TIMEOUT) {
      if(monitor.getExitCode() == 0) {
        build.setOutcome(BuildOutcome.SUCCESS);
      } else if(monitor.getExitCode() == 1) {
        build.setOutcome(BuildOutcome.FAILURE);
      } else {
        System.out.println(pkg + " exited with code " + monitor.getExitCode());
        build.setOutcome(BuildOutcome.ERROR);
      }
    }
  }

  /**
   * Check whether the source is already unpacked in our workspace,
   * otherwise download and unpack
   */
  private void ensureSourceUnpacked() throws IOException {

    if(!baseDir.exists()) {

      InputStream in = GoogleCloudStorage.INSTANCE.openSourceArchive(
          pkg.getGroupId(),
          pkg.getPackage().getName(),
          pkg.getVersion());

      TarArchiveInputStream tarIn = new TarArchiveInputStream(
        new GZIPInputStream(in));

      String packagePrefix = pkg.getPackageName() + "/";

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
}
