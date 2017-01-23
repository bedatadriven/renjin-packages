package org.renjin.ci.jenkins.benchmark;

import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

public class DefaultBlas implements BlasLibrary {


  @Override
  public String getConfigureFlags() {
    return "";
  }
  
  @Override
  public String getName() {
    return "reference";
  }

  @Override
  public String getNameAndVersion() {
    return getName();
  }

  @Override
  public String getLibraryPath() {
    return null;
  }

  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) {
    
  }

}
