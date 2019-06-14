package org.renjin.ci.gradle;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.renjin.ci.gradle.graph.PackageNode;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.storage.StorageKeys;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class PackageSetup implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(PackageSetup.class.getName());

  private final PackageNode node;
  private final File packageDir;
  private final BuildFileWriter writer;

  public PackageSetup(PackageNode node, File packageDir, BuildFileWriter writer) {
    this.node = node;
    this.packageDir = packageDir;
    this.writer = writer;
  }


  @Override
  public void run() {

    if(packageDir.exists() && !correctVersionDownloaded()) {
      run("rm", "-rf", packageDir.getAbsolutePath());
    }

    if(!packageDir.exists()) {
      boolean created = packageDir.mkdirs();
      if(!created) {
        throw new RuntimeException("Could not create directory at " + packageDir.getAbsolutePath());
      }
    }

    String archiveFileName = node.getId().getPackageName() +  "_" + node.getId().getVersionString() + ".tgz";
    File archiveFile = new File(packageDir.getParentFile(), archiveFileName);

    if(correctVersionDownloaded()) {
      LOGGER.info(node.getId() + " already downloaded, skipping.");
    } else {
      downloadAndUnpackSources(archiveFile);
    }

    // Update build.gradle
    File buildFile = new File(packageDir, "build.gradle");
    try(PrintWriter printWriter = new PrintWriter(buildFile)) {
      writer.write(node, packageDir, printWriter);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Exception writing build.gradle for " + node.getId(), e);
    }
  }

  private void downloadAndUnpackSources(File archiveFile) {
    // Download from GCS
    run("gsutil", "-q", "cp", getGsUrl(), archiveFile.getAbsolutePath());

    // Unpack into directory
    run("tar", "-xzf", archiveFile.getAbsolutePath(), "--strip", "1");

    // Clean up zip file
    archiveFile.delete();
  }

  private boolean correctVersionDownloaded() {

    File descriptionFile = new File(packageDir, "DESCRIPTION");
    if(!descriptionFile.exists()) {
      return false;
    }

    PackageDescription description;
    try {
      description = PackageDescription.fromCharSource(Files.asCharSource(descriptionFile, Charsets.UTF_8));
    } catch (IOException e) {
      return false;
    }

    return node.getId().getVersionString().equals(description.getVersion());
  }

  private void run(String... commandLine)  {
    int status;

    try {
      status = new ProcessBuilder(commandLine)
          .inheritIO()
          .directory(packageDir)
          .start()
          .waitFor();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while executing: " + Arrays.toString(commandLine));
    } catch (IOException e) {
      throw new RuntimeException("IOException while executing: " + Arrays.toString(commandLine), e);
    }

    if(status != 0) {
      throw new RuntimeException("Status code " + status + " from executing " + Arrays.toString(commandLine));
    }
  }

  private String getGsUrl() {
    return "gs://" + StorageKeys.PACKAGE_SOURCE_BUCKET + "/" + StorageKeys.packageSource(node.getId());
  }
}
