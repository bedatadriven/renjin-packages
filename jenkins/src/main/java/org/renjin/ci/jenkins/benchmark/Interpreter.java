package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Common interface to R Interpreters
 */
public abstract class Interpreter {
  abstract void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;

  public abstract String getId();
  
  public abstract String getVersion();
  
  public abstract boolean execute(Launcher launcher, TaskListener listener, Node node,
                                  FilePath runScript, List<PackageVersionId> dependencies,
                                  boolean dryRun, long timeoutMillis) throws IOException, InterruptedException;
  
  public Map<String, String> getRunVariables() {
    return Collections.emptyMap();
  }
  
}
