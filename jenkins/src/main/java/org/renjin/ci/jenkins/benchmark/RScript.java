package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs a script
 */
public class RScript {
  
  public FilePath bin;
  private String userLibsEnvVariable = "R_LIBS_USER";

  /**
   * Path to the Rscript binary
   * @param bin
   */
  public RScript(FilePath bin) {
    this.bin = bin;
  }

  public void setUserLibsEnvVariable(String userLibsEnvVariable) {
    this.userLibsEnvVariable = userLibsEnvVariable;
  }

  public Launcher.ProcStarter runScript(Launcher launcher, FilePath libraryPath, FilePath scriptPath)
      throws IOException, InterruptedException {

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(bin.getRemote());
    args.add(scriptPath.getName());

    Map<String, String> env = new HashMap<String, String>();
    env.put(userLibsEnvVariable, libraryPath.getRemote());

    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args)
        .pwd(scriptPath.getParent())
        .envs(env);
    
    return ps;
  }
  
  
}
