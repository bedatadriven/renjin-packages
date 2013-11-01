package org.renjin.cran;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.repo.model.BuildOutcome;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Callable (concurrent) task that builds a package.
 */
public class PackageBuilder implements Callable<BuildResult> {

  private static final Logger LOGGER = Logger.getLogger(PackageBuilder.class.getName());
  private final Workspace workspace;
  private final BuildReporter reporter;
  private final File baseDir;

  private PackageNode pkg;
  private File logFile;

  public static final long TIMEOUT_SECONDS = 20 * 60;

  public static final int MAX_LOG_SIZE = 1024 * 200;

  public PackageBuilder(Workspace workspace, BuildReporter reporter, PackageNode pkg) {
    this.workspace = workspace;
    this.reporter = reporter;
    this.pkg = pkg;
    this.baseDir = new File(workspace.getPackagesDir(), pkg.getName() + "_" + pkg.getVersion());
    this.logFile = new File(baseDir, "build.log." + reporter.getBuildId());
  }

  @Override
  public BuildResult call() throws Exception {
   
    // set the name of this thread to the package
    // name for debugging
    Thread.currentThread().setName(pkg.getName());

    BuildResult result = new BuildResult();
    result.setPackageVersionId(pkg.getId());

    try {
      executeMaven(result);

    } catch(Exception e) {
      System.out.println("Exception building " + pkg.getId() + ": " + e.getMessage());
      result.setOutcome(BuildOutcome.ERROR);
    }

    try {
      reporter.reportResult(pkg, result.getOutcome(), baseDir, logFile);
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception recording build results for " + pkg.getId(), e);
    }

    return result; 
  }

  private void executeMaven(BuildResult result) throws IOException, InterruptedException {

    ensureUnpacked();

    // write out the POM file for this package
    PomBuilder pomBuilder = new PomBuilder(workspace.getRenjinVersion(), baseDir, pkg.getEdges());
    pomBuilder.writePom();

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

    command.add("-DskipTests");

    command.add("clean");
    command.add("install");

    ProcessBuilder builder = new ProcessBuilder(command);

    builder.directory(baseDir);
    builder.redirectErrorStream(true);

    long startTime = System.currentTimeMillis();
    Process process = builder.start();

    InputStream processOutput = process.getInputStream();
    OutputCollector collector = new OutputCollector(processOutput, logFile, MAX_LOG_SIZE);
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
  }


  /**
   * Check whether the source is already unpacked in our workspace,
   * otherwise download and unpack
   */
  private void ensureUnpacked() throws IOException {

    if(!baseDir.exists()) {

      InputStream in = GoogleCloudStorage.INSTANCE.openSourceArchive(pkg.getGroupId(),
        pkg.getName(), pkg.getVersion());

      TarArchiveInputStream tarIn = new TarArchiveInputStream(
        new GZIPInputStream(in));

      String packagePrefix = pkg.getName() + "/";

      TarArchiveEntry entry;
      while((entry=tarIn.getNextTarEntry())!=null) {
        if(entry.isFile() && entry.getName().startsWith(packagePrefix)) {

          String name = entry.getName().substring(packagePrefix.length());

          File outFile = new File(baseDir.getAbsolutePath() + File.separator + name);
          outFile.getParentFile().mkdirs();

          ByteStreams.copy(tarIn, Files.newOutputStreamSupplier(outFile));
        }
      }
      tarIn.close();
    }
  }

  private String getMavenPath() {
    if(System.getProperty("os.name").toLowerCase().contains("windows")) {
      return "mvn.bat";
    } else {
      return "mvn";
    }
  }
}
