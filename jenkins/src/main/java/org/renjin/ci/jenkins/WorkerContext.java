package org.renjin.ci.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.WorkspaceList;

import java.io.IOException;
import java.io.PrintStream;


public class WorkerContext {

  private final Run<?, ?> run;
  private final EnvVars env;
  private final Node node;
  private final Launcher launcher;
  private final FilePath workspace;
  private final TaskListener listener;


  public WorkerContext(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
    this.run = run;
    this.listener = listener;

    Executor exec = Executor.currentExecutor();
    if (exec == null) {
      throw new IllegalStateException("running task without associated executor thread");
    }
    Computer computer = exec.getOwner();

    env = computer.getEnvironment();

    node = computer.getNode();
    if (node == null) {
      throw new IllegalStateException("running computer lacks a node");
    }
    launcher = node.createLauncher(listener);

    // Create a workspace for this build
    Job<?,?> j = run.getParent();
    if (!(j instanceof TopLevelItem)) {
      throw new IllegalStateException(j + " must be a top-level job");
    }
    FilePath path = node.getWorkspaceFor((TopLevelItem) j);
    if (path == null) {
      throw new IllegalStateException(node + " is offline");
    }
    WorkspaceList.Lease lease = computer.getWorkspaceList().allocate(path);
    workspace = lease.path;
  }

  public Run<?, ?> getRun() {
    return run;
  }

  public EnvVars getEnv() {
    return env;
  }

  public Node getNode() {
    return node;
  }

  public Launcher getLauncher() {
    return launcher;
  }

  public FilePath getWorkspace() {
    return workspace;
  }

  public PrintStream getLogger() {
    return listener.getLogger();
  }

  public FilePath child(String relativeOrAbsolutePath) {
    return workspace.child(relativeOrAbsolutePath);
  }

  public Job<?, ?> getJob() {
    return run.getParent();
  }

  public TaskListener getListener() {
    return listener;
  }

  public void log(String format, Object... arguments) {
    getLogger().println(String.format(format, arguments));
  }
}
