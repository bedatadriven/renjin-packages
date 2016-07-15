package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a private library path for a specific combination of dependencies.
 */
public class LibraryDir {

  private String interpreter;
  private String interpreterVersion;
  private List<PackageVersionId> dependencies;
  private String libraryName;
  private FilePath libraryPath;

  public LibraryDir(String interpreter, String interpreterVersion, List<PackageVersionId> dependencies) {
    this.interpreter = interpreter;
    this.interpreterVersion = interpreterVersion;
    this.dependencies = dependencies;
    this.libraryName = computeLibraryName();  
  }

  private String computeLibraryName() {
    Hasher hasher = Hashing.md5().newHasher();
    hasher.putString(interpreter);
    hasher.putString(interpreterVersion);
    for (PackageVersionId dependency : dependencies) {
      hasher.putString(dependency.toString());
    }
    return hasher.hash().toString();
  }

  public void ensureInstalled(Launcher launcher, TaskListener listener, Node node, RScript executor) 
      throws IOException, InterruptedException {
    libraryPath = node.getRootPath().child("gnur-libraries").child(libraryName);

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

        // Send output to install log
        ByteArrayOutputStream installLog = new ByteArrayOutputStream();

        int exitCode = executor.runScript(launcher, libraryPath, installScriptPath)
            .stdout(installLog)
            .stderr(installLog)
            .start()
            .join();


        // Write install log to directory
        FilePath logFile = libraryPath.child("install.log");
        OutputStream output = logFile.write();
        ByteStreams.copy(new ByteArrayInputStream(installLog.toByteArray()), output);
        output.close();
        
        // Clean up archives
        archivePath.deleteRecursive();
        
        if (exitCode != 0) {
          throw new RuntimeException("Failed to build library for: " + dependencies);
        }

        libraryPath.child(".installed").touch(System.currentTimeMillis());
      }
    }
  }

  public FilePath getPath() {
    return libraryPath;
  }
}
