package org.renjin.ci.jenkins.benchmark;

import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * Uses netlib's pure-java F2J compiled BLAS library
 */
public class F2JBlas implements BlasLibrary {
  @Override
  public String getConfigureFlags() {
    throw new UnsupportedOperationException("F2JBlas cannot be used to build GNU R");
  }

  @Override
  public String getName() {
    return "f2jblas";
  }

  @Override
  public String getNameAndVersion() {
    return "f2jblas";
  }

  @Override
  public String getLibraryPath() {
    return null;
  }

  @Override
  public String getBlasSharedLibraryPath() {
    return null;
  }

  @Override
  public String getCompilationId() {
    return "0";
  }

  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    // NOOP, pulled in by Maven
  }
}
