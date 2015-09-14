package org.renjin.ci.jenkins;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.tools.Maven;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Context for an individual package build
 */
public class BuildContext {
  private WorkerContext workerContext;
  private Maven maven;
  private PackageNode packageNode;
  private FilePath buildDir;

  /**
   * Local gzip'd log file
   */
  private File logFile;

  public BuildContext(WorkerContext workerContext, Maven maven, PackageNode packageNode) throws IOException, InterruptedException {
    this.workerContext = workerContext;
    this.maven = maven;
    this.packageNode = packageNode;
    this.buildDir = workerContext.child("package");
    if(this.buildDir.exists()) {
      buildDir.deleteRecursive();
    }
    try {
      buildDir.mkdirs();
    } catch (IOException e) {
      // sometimes this seems to randomly fail...
      // wait a moment and then try again...
      Thread.sleep(2000);
      buildDir.mkdirs();
    }
    logFile = File.createTempFile("package", ".log.gz");
  }

  public FilePath getBuildDir() {
    return buildDir;
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
}
