package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Installs and runs a version of the GNU R interpreter
 */
public class GnuR extends Interpreter {
  

  protected final String version;

  private FilePath bin;
  private FilePath sourcePath;
  
  public GnuR(String version) {
    this.version = version;
  }

  @Override
  final void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    
    FilePath homePath = node.getRootPath().child("tools").child(installPrefix()).child(version);
    sourcePath = homePath.child(sourceDirectoryName());

    // check if installation is complete
    if(!homePath.child(".installed").exists()) {


      // Download and install the source
      homePath.installIfNecessaryFrom(getSourceUrl(), taskListener, "Installing " + installPrefix() + version + " to " + homePath);

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
        throw new AbortException("./configure failed for " + installPrefix() + " " + version);
      }

      int buildExitCode = launcher.launch()
          .pwd(sourcePath)
          .cmdAsSingleString("make")
          .stdout(taskListener)
          .start()
          .join();

      if (buildExitCode != 0) {
        throw new AbortException("make failed for " + installPrefix() + version);
      }

      homePath.child(".installed").touch(System.currentTimeMillis());
    }
    
    bin = sourcePath.child("bin").child("Rscript");
  }

  /**
   * Excludes dependencies like "Matrix" that are installed along with the version of R.
   */
  private List<PackageVersionId> excludeInstalled(List<PackageVersionId> dependencies) throws IOException, InterruptedException {
    FilePath libraryPath = sourcePath.child("library");
    
    List<PackageVersionId> toInstall = new ArrayList<PackageVersionId>();
    for (PackageVersionId dependency : dependencies) {
      if(!libraryPath.child(dependency.getPackageName()).exists()) {
        toInstall.add(dependency);
      }
    }
    return toInstall;
  }

  protected String sourceDirectoryName() {
    return "R-" + version;
  }

  private String installPrefix() {
    return getId().replace(' ', '_');
  }

  protected URL getSourceUrl() throws AbortException {
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

  private FilePath buildLibrary(Launcher launcher, TaskListener listener, Node node, List<PackageVersionId> dependencies) throws IOException, InterruptedException {
    String libraryName = computeLibraryName(dependencies);
    FilePath libraryPath = node.getRootPath().child("gnur-libraries").child(libraryName);

    listener.getLogger().println("Building library " + libraryName + " for dependencies: " + dependencies);
    
    if(dependencies.size() > 0) {
      if (!libraryPath.child(".installed").exists()) {

        // First download the source archives
        List<FilePath> archiveFiles = new ArrayList<FilePath>();
        FilePath archivePath = libraryPath.child(".archives");
        archivePath.mkdirs();
        
        for (PackageVersionId dependency : dependencies) {
          FilePath archiveFile = archivePath.child(dependency.getPackageName() + "_" + dependency.getVersionString() + ".tar.gz");
          URL archiveUrl = new URL(String.format("https://storage.googleapis.com/renjinci-package-sources/%s/%s_%s.tar.gz",
              dependency.getGroupId(),
              dependency.getPackageName(),
              dependency.getVersionString()));
          
          archiveFile.copyFrom(archiveUrl);
          archiveFiles.add(archiveFile);
        }
        
        // Now compose a script that will install from the sources
        StringBuilder installScript = new StringBuilder();
        installScript.append("install.packages(repos=NULL, pkgs=c(");

        boolean needsComma = false;
        for (FilePath archiveFile : archiveFiles) {
          if(needsComma) {
            installScript.append(", ");
          }
          installScript.append("\n").append("'").append(archiveFile.getRemote()).append("'");
          needsComma = true;
        }
        installScript.append("))\n");
        
        // Execute installation script
        FilePath installScriptPath = libraryPath.child("install.R");
        installScriptPath.write(installScript.toString(), Charsets.UTF_8.name());

        int exitCode = runScript(launcher, listener, libraryPath, installScriptPath);
        if (exitCode != 0) {
          throw new RuntimeException("Failed to build library for: " + dependencies);
        }

        libraryPath.child(".installed").touch(System.currentTimeMillis());
      }
    }
    
    return libraryPath;
  }

  private int runScript(Launcher launcher, TaskListener listener, FilePath libraryPath, FilePath scriptPath) 
      throws IOException, InterruptedException {

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(bin.getRemote());
    args.add(scriptPath.getName());

    Map<String, String> env = new HashMap<String, String>();
    env.put("R_LIBS_USER", libraryPath.getRemote());
    
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args)
        .pwd(scriptPath.getParent())
        .envs(env)
        .stdout(listener);

    Proc proc = launcher.launch(ps);
    int exitCode = proc.join();

    listener.getLogger().println("Exit code : " + exitCode);
    
    return exitCode;
  }
  
  private String computeLibraryName(List<PackageVersionId> dependencies) {
    Hasher hasher = Hashing.md5().newHasher();
    hasher.putString(getId());
    hasher.putString(getVersion());
    for (PackageVersionId dependency : dependencies) {
      hasher.putString(dependency.toString());
    }
    return hasher.hash().toString();
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener,
                         Node node, FilePath runScript, List<PackageVersionId> dependencies, boolean dryRun) throws IOException, InterruptedException {

    FilePath libraryPath = buildLibrary(launcher, listener, node, excludeInstalled(dependencies));
    
    if(dryRun) {
      return true;
    } else {
      int exitCode = runScript(launcher, listener, libraryPath, runScript);
      return exitCode == 0;
    }
    
  }

}
