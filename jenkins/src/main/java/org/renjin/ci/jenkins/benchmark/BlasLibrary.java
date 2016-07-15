package org.renjin.ci.jenkins.benchmark;


import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;

public interface BlasLibrary {
  
  String getConfigureFlags();

  String getName();

  String getNameAndVersion();
  
  void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;

}
