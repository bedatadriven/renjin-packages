package org.renjin.ci.jenkins.benchmark;


import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;

public interface BlasLibrary {
  
  String getConfigureFlags();

  String getName();

  String getNameAndVersion();

  /**
   * @return the directory where this blas library can be found, or {@code null} if no library path is required.
   */
  String getLibraryPath();

  void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;

}
