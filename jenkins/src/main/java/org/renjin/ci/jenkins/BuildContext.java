package org.renjin.ci.jenkins;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.tools.Maven;
import org.renjin.ci.model.PackageVersionId;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Context for an individual package build
 */
public class BuildContext {
  
  private static final Logger LOGGER = Logger.getLogger(BuildContext.class.getName());
  
  private WorkerContext workerContext;
  private Maven maven;
  private PackageNode packageNode;
  private FilePath buildDir;
  private FilePath plotDir;
  private String buildNumber;

  /**
   * Local gzip'd log file
   */
  private File logFile;

  public BuildContext(WorkerContext workerContext, Maven maven, PackageNode packageNode, String buildNumber)
      throws IOException, InterruptedException {

    this.workerContext = workerContext;
    this.maven = maven;
    this.packageNode = packageNode;
    this.buildDir = workerContext.getWorkspace();
    this.buildNumber = buildNumber;
    if(this.buildDir.exists()) {
      buildDir.deleteRecursive();
    }
    try {
      buildDir.mkdirs();
    } catch (IOException e) {
      // sometimes this seems to randomly fail...
      // wait a moment and then try again...
      Thread.sleep(2000);
      try {
        buildDir.mkdirs();
      } catch (Exception secondException) {
        throw new IOException("Failed to create working directory on " + workerContext.getNode().getDisplayName(), e);
      }
    }

    plotDir = buildDir.child("plots");
    plotDir.mkdirs();

    logFile = File.createTempFile("package", ".log.gz");
  }

  public FilePath getBuildDir() {
    return buildDir;
  }

  public FilePath getPlotDir() {
    return plotDir;
  }

  public void setBuildDir(FilePath buildDir) {
    this.buildDir = buildDir;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public WorkerContext getWorkerContext() {
    return workerContext;
  }

  public Maven getMaven() {
    return maven;
  }

  public PrintStream getLogger() {
    return workerContext.getLogger();
  }
  

  public TaskListener getListener() {
    return workerContext.getListener();
  }

  public File getLogFile() {
    return logFile;
  }
  
  public void cleanup() {
    try {
      workerContext.getWorkspace().deleteRecursive();
    } catch (Exception e) {
      LOGGER.severe("Failed to delete build directory " + buildDir);
    }
    if(logFile.exists()) {
      logFile.delete();
    }
  }

  public void log(String message, Object... arguments) {
    workerContext.getLogger().println(packageNode.getLabel() + ": " + String.format(message, arguments));
  }
  
  public ByteSource getLogAsByteSource() {
    return new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        if(logFile.exists()) {
          return new GZIPInputStream(new FileInputStream(logFile));
        } else {
          return new ByteArrayInputStream(new byte[0]);
        }
      }
    };
  }
  
  public CharSource getLogAsCharSource() {
    return getLogAsByteSource().asCharSource(Charsets.UTF_8);
  }

  public PackageNode getPackageNode() {
    return packageNode;
  }

  public PackageVersionId getPackageVersionId() {
    return packageNode.getId();
  }
}
