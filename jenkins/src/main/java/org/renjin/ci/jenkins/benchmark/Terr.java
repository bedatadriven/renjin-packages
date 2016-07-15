package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tibco interpreter
 */
public class Terr extends Interpreter {
  
  private String version;
  private FilePath scriptBin;

  public Terr(String version) {
    this.version = version;
  }

  @Override
  void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    FilePath homeDir = node.getRootPath().child("tools").child("terr").child(version);
    scriptBin = homeDir.child("bin").child("TERRscript");
    if(!scriptBin.exists()) {
      throw new RuntimeException("Could not find TERRscript at " + scriptBin.getRemote());
    }
  }

  @Override
  public String getId() {
    return "TERR";
  }

  @Override
  public Map<String, String> getRunVariables() {
    Map<String, String> variables = new HashMap<String, String>();
    variables.put(RunVariables.BLAS, "MKL");
    return variables;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener, Node node, 
                         FilePath runScript, List<PackageVersionId> dependencies, boolean dryRun) 
            throws IOException, InterruptedException {
    
    RScript rScript = new RScript(scriptBin);
    rScript.setUserLibsEnvVariable("TERR_LIBS_USER");
    
    LibraryDir libraryDir = new LibraryDir(getId(), version, dependencies);
    libraryDir.ensureInstalled(launcher, listener, node, rScript);
    
    int exitCode = rScript.runScript(launcher, libraryDir.getPath(), runScript)
        .stdout(listener)
        .start()
        .join();
    
    return exitCode == 0;
  }
}
