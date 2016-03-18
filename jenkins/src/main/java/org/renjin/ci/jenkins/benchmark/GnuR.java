package org.renjin.ci.jenkins.benchmark;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Installs and runs a version of the GNU R interpreter
 */
public class GnuR extends Interpreter {

  private String version;

  private FilePath bin;
  
  public GnuR(String version) {
    this.version = version;
  }

  @Override
  void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    
    FilePath homePath = node.getRootPath().child("tools").child("GNU_R").child(version);
    FilePath sourcePath = homePath.child("R-" + version);

    // check if installation is complete
    if(!homePath.child(".installed").exists()) {


      // Download and install the source
      homePath.installIfNecessaryFrom(getSourceUrl(), taskListener, "Installing GNU R " + version + " to " + homePath);

      if (!sourcePath.exists()) {
        throw new AbortException("Expected source directory not found: " + sourcePath);
      }
      // Configure && build
      int configureExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString("./configure")
          .stdout(taskListener)
          .start()
          .join();

      if (configureExitCode != 0) {
        throw new AbortException("./configure failed for GNU R" + version);
      }

      int buildExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString("make")
          .stdout(taskListener)
          .start()
          .join();

      if (buildExitCode != 0) {
        throw new AbortException("make failed for GNU R " + version);
      }

      homePath.child(".installed").touch(System.currentTimeMillis());
    }
    
    bin = sourcePath.child("bin").child("R");
  }

  private URL getSourceUrl() throws AbortException {
    String versionParts[] = version.split("\\.");
    if(versionParts.length != 3) {
      throw new AbortException("Invalid GNU R version: " + version);
    }
    try {
      return new URL(String.format("https://cran.r-project.org/src/base/R-%s/R-%s.tar.gz", versionParts[0], version));
    } catch (MalformedURLException e) {
      throw new AbortException("Malformed URL for GNU R version " + version);
    }
  }

  @Override
  public String getId() {
    return "GNU R";
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener, FilePath runScript) throws IOException, InterruptedException {
    return execute(launcher, listener,  bin, runScript);
  }
}
