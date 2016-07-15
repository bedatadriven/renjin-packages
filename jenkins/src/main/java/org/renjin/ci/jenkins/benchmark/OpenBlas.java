package org.renjin.ci.jenkins.benchmark;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class OpenBlas implements BlasLibrary {
  
  private final String version;
  
  private FilePath sourcePath;

  public OpenBlas() {
    version = "0.2.18";
  }
  
  public OpenBlas(String version) {
    this.version = version;
  }
  
  public URL getDownloadUrl() throws MalformedURLException {
    return new URL("https://codeload.github.com/xianyi/OpenBLAS/tar.gz/v" + version);
  }
  
  @Override
  public String getConfigureFlags() {
    return String.format("--with-blas=\"-L%s -lopenblas\"", sourcePath.getRemote());
  }


  @Override
  public String getName() {
    return "OpenBLAS";
  }

  @Override
  public String getNameAndVersion() {
    return "OpenBLAS-" + version;
  }

  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    FilePath homePath = node.getRootPath().child("tools").child("OpenBLAS-" + version).child(version);
    sourcePath = homePath.child("OpenBLAS-" + version);

    // check if installation is complete
    if(!homePath.child(".installed").exists()) {

      // Download and install the source
      homePath.installIfNecessaryFrom(getDownloadUrl(), taskListener, "Installing OpenBLAS " + version + " to " + homePath);

      if (!sourcePath.exists()) {
        throw new AbortException("Expected source directory not found: " + sourcePath);
      }
      
      // Make 
      int buildExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString("make")
          .stdout(taskListener)
          .start()
          .join();

      if (buildExitCode != 0) {
        throw new AbortException("make failed for OpenBLAS " + version);
      }

      homePath.child(".installed").touch(System.currentTimeMillis());
    }
  }
}
