package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Preconditions;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Executes Renjin
 */
public class Renjin extends Interpreter {

  private String version;
  private FilePath bin;

  public Renjin(FilePath workspace, TaskListener listener, String version) {
    this.version = version;
  }
  
  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

    URL renjinArchive;
    try {
      renjinArchive = new URL("https://nexus.bedatadriven.com/content/groups/public/org/renjin/renjin-generic-package/"
          + version + "/renjin-generic-package-" + version + ".zip");
    } catch (MalformedURLException e) {
      e.printStackTrace(listener.getLogger());
      throw new AbortException();
    }

    FilePath renjinLocation = node.getRootPath().child("tools").child("Renjin").child("renjin-" + version);
    renjinLocation.installIfNecessaryFrom(renjinArchive, listener, "Installing Renjin " + version + "...");

    List<FilePath> subDirs = renjinLocation.listDirectories();
    if(subDirs.size() != 1) {
      throw new AbortException("Error installing Renjin " + version + ", expected exactly one sub directory, found " + 
          subDirs);
    }
    
    FilePath subDir = subDirs.get(0);
    
    bin = subDir.child("bin").child("renjin");

    if(!bin.exists()) {
      listener.fatalError("Renjin executable " + bin + " does not exist!");
      throw new AbortException();
    }
  }

  @Override
  public String getId() {
    return "Renjin";
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener, Node node, FilePath scriptPath, List<PackageVersionId> dependencies, boolean dryRun) throws IOException, InterruptedException {
    Preconditions.checkState(bin != null);
    
    return execute(launcher, listener, bin, scriptPath);
  }
}
