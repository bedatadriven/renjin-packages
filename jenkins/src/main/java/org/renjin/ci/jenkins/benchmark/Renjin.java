package org.renjin.ci.jenkins.benchmark;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Executes Renjin
 */
public class Renjin extends Interpreter {

  private FilePath workspace;
  private TaskListener listener;
  private String version;
  
  private FilePath bin;

  public Renjin(FilePath workspace, TaskListener listener, String version) {
    this.workspace = workspace;
    this.listener = listener;
    this.version = version;
  }
  
  @Override
  public void ensureInstalled() throws IOException, InterruptedException {

    URL renjinArchive;
    try {
      renjinArchive = new URL("https://nexus.bedatadriven.com/content/groups/public/org/renjin/renjin-generic-package/"
          + version + "/renjin-generic-package-" + version + ".zip");
    } catch (MalformedURLException e) {
      e.printStackTrace(listener.getLogger());
      throw new AbortException();
    }

    FilePath renjinLocation = workspace.child("bin").child("renjin-" + version);
    renjinLocation.installIfNecessaryFrom(renjinArchive, listener, "Installing Renjin " + version + "...");

    bin = renjinLocation.child("renjin-" + version).child("bin").child("renjin");

    if(!bin.exists()) {
      listener.fatalError("Renjin executable " + bin + " does not exist!");
      throw new AbortException();
    }
  }

  @Override
  public void execute(Launcher launcher, FilePath scriptPath) throws IOException, InterruptedException {

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(bin.getRemote());
    args.add("-f");
    args.add(scriptPath.getName());

    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).pwd(scriptPath.getParent()).stdout(listener);

    Proc proc = launcher.launch(ps);
    int exitCode = proc.join();
    
    listener.getLogger().println("Exit code : " + exitCode);
  }
}
