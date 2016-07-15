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

  /**
   * Path to the Rscript binary
   * @param bin
   */
  public RScript(FilePath bin) {
    this.bin = bin;
  }

  public Launcher.ProcStarter runScript(Launcher launcher, FilePath libraryPath, FilePath scriptPath)
      throws IOException, InterruptedException {

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(bin.getRemote());
    args.add(scriptPath.getName());

    Map<String, String> env = new HashMap<String, String>();
    env.put("R_LIBS_USER", libraryPath.getRemote());

    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args)
        .pwd(scriptPath.getParent())
        .envs(env);
    
    return ps;
  }
  

}
