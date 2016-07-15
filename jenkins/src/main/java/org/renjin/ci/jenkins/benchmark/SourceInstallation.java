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
 * Installs an interpreter from source
 */
public class SourceInstallation {

  private String installPrefix;
  private String version;
  private URL sourceUrl;
  private String sourceDirectoryName;
  
  private FilePath sourcePath;
  
  private BlasLibrary blasLibrary;
  private String gccVersion;

  public SourceInstallation(BlasLibrary blasLibrary) {
    this.blasLibrary = blasLibrary;
  }
  
  public SourceInstallation() {
    this.blasLibrary = new DefaultBlas();
  }

  /**
   * The prefix to use for this interpreter in the tools/ directory
   */
  public void setInstallPrefix(String installPrefix) {
    this.installPrefix = installPrefix;
  }

  /**
   * The version of this interpreter to install
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * The URL of the archive containing the sources
   */
  public void setSourceUrl(String sourceUrl) {
    try {
      this.sourceUrl = new URL(sourceUrl);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url: " + sourceUrl, e);
    }
  }

  /**
   * The name of the source directory within the tar.gz archive.
   */
  public void setSourceDirectoryName(String sourceDirectoryName) {
    this.sourceDirectoryName = sourceDirectoryName;
  }


  public void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {

    gccVersion = VersionDetectors.detectGccVersion(launcher);
    
    blasLibrary.ensureInstalled(node, launcher, taskListener);
    
    FilePath homePath = node.getRootPath().child("tools").child(installPrefix + "-" + blasLibrary.getNameAndVersion()).child(version);
    sourcePath = homePath.child(sourceDirectoryName);

    // check if installation is complete
    if(!homePath.child(".installed").exists()) {


      // Download and install the source
      homePath.installIfNecessaryFrom(sourceUrl, taskListener, "Installing " + installPrefix + version + " to " + homePath);

      if (!sourcePath.exists()) {
        throw new AbortException("Expected source directory not found: " + sourcePath);
      }

      String commandLine = "./configure";
      if(!blasLibrary.getConfigureFlags().isEmpty()) {
        commandLine += " " + blasLibrary.getConfigureFlags();
      }
      
      // Configure && build
      int configureExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString(commandLine)
          .stdout(taskListener)
          .start()
          .join();

      if (configureExitCode != 0) {
        throw new AbortException("./configure failed for " + installPrefix + " " + version);
      }

      int buildExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString("make")
          .stdout(taskListener)
          .start()
          .join();

      if (buildExitCode != 0) {
        throw new AbortException("make failed for " + installPrefix + version);
      }

      homePath.child(".installed").touch(System.currentTimeMillis());
    }
  }

  public RScript getExecutor() {
    return new RScript(sourcePath.child("bin").child("Rscript"));
  }

  public String getGccVersion() {
    return gccVersion;
  }
}
