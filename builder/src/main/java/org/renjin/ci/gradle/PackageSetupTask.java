package org.renjin.ci.gradle;

import com.google.api.client.util.Lists;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import org.renjin.ci.model.CorePackages;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Downloads source and writes build file for an individual package.
 */
public class PackageSetupTask implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(PackageSetupTask.class.getName());

  private final PackageIndex packageIndex;
  private final PackageVersionId id;
  private final File packageDir;

  public PackageSetupTask(PackageIndex packageIndex, PackageVersionId id, File packageDir) {
    this.packageIndex = packageIndex;
    this.id = id;
    this.packageDir = packageDir;
  }

  @Override
  public void run() {

    try {

      if (packageDir.exists() && !correctVersionDownloaded()) {
        run("rm", "-rf", packageDir.getAbsolutePath());
      }

      if (!packageDir.exists()) {
        boolean created = packageDir.mkdirs();
        if (!created) {
          throw new RuntimeException("Could not create directory at " + packageDir.getAbsolutePath());
        }
      }

      String archiveFileName = id.getPackageName() + "_" + id.getVersionString() + ".tgz";
      File archiveFile = new File(packageDir.getParentFile(), archiveFileName);

      if (correctVersionDownloaded()) {
        LOGGER.info(id + " already downloaded, skipping.");
      } else {
        checkForPatchedRepo();
        downloadAndUnpackSources(archiveFile);
      }

      // Update build.gradle
      File buildFile = new File(packageDir, "build.gradle");
      try (PrintWriter printWriter = new PrintWriter(buildFile)) {
        writeBuildFile(printWriter);
      } catch (FileNotFoundException e) {
        throw new RuntimeException("Exception writing build.gradle for " + id, e);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void checkForPatchedRepo() {
    File gitRepo = new File(packageDir, ".git");
    if(gitRepo.exists()) {
      throw new RuntimeException("Package source repo at " + packageDir + " does not match requested version");
    }
  }

  private void downloadAndUnpackSources(File archiveFile) {

    LOGGER.info("Downloading " + id + "...");

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

    return id.getVersionString().equals(description.getVersion());
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
    return "gs://" + StorageKeys.PACKAGE_SOURCE_BUCKET + "/" + StorageKeys.packageSource(id);
  }


  private void writeBuildFile(PrintWriter writer) {


    File descriptionFile = new File(packageDir, "DESCRIPTION");
    PackageDescription description;
    try {
      description = PackageDescription.fromCharSource(Files.asCharSource(descriptionFile, Charsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    writer.println("group = '" + id.getGroupId() + "'");
    writer.println();
    writer.println("apply plugin: 'org.renjin.package'");

    boolean blacklisted = packageIndex.getBlacklist().isBlacklisted(id.getPackageName());
    boolean needsCompilation = description.isNeedsCompilation() && !blacklisted;

    if(needsCompilation) {
      writer.println("apply plugin: 'org.renjin.native-sources'");
      maybeApplyCxxStandard(writer, description);
    }

    writer.println();
    writer.println("dependencies {");

    addDependency(writer, description.getDepends(), "compile");
    addDependency(writer, description.getImports(), "compile");
    if(needsCompilation) {
      addDependency(writer, description.getLinkingTo(), "link");
    }
    addDependency(writer, nonBlacklistedSuggests(description), "testRuntime");

    if(needsCompilation && hasCplusplusSources(packageDir)) {
      writer.println("  compile 'org.renjin:libstdcxx:4.7.4-b34'");
    }
    if(id.getPackageName().equals("testthat")) {
      writer.println("  compile 'org.renjin.cran:xml2:+'");
    }

    writer.println("}");


    if(blacklisted) {
      writer.println();
      writer.println("configure.enabled = false");
      writer.println("testNamespace.enabled = false");
    }
  }

  private void maybeApplyCxxStandard(PrintWriter writer, PackageDescription description) {

    String cxxStandard = detectCxxStandard(description);
    if(!cxxStandard.isEmpty()) {
      writer.println();
      writer.println("make.cxxStandard = \"" + cxxStandard + "\"");
    }
  }

  private String detectCxxStandard(PackageDescription description) {
    if(Strings.nullToEmpty(description.getSystemRequirements()).contains("C++11")) {
      return "C++11";
    }
    File makeVars = new File(packageDir, "src/Makevars");
    if(makeVars.exists()) {
      List<String> lines;
      try {
        lines = Files.readLines(makeVars, Charsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException("Exception reading " + makeVars.getAbsolutePath(), e);
      }
      for (String line : lines) {
        if(line.matches("^CXX_STD\\s*=\\s*CXX11")) {
          return "C++11";
        }
      }
    }
    return "";
  }

  private Iterable<PackageDependency> nonBlacklistedSuggests(PackageDescription description) {
    return Lists.newArrayList(description.getSuggests())
      .stream()
      .filter(d -> !packageIndex.getBlacklist().isBlacklisted(d.getName()))
      .collect(Collectors.toList());
  }

  private boolean hasCplusplusSources(File packageDir) {

    File srcDir = new File(packageDir, "src");
    File[] files = srcDir.listFiles();
    if(files != null) {
      for (File file : files) {
        String fileName = file.getName();
        if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".C") || fileName.endsWith(".c++")) {
          return true;
        }
      }
    }
    return false;
  }

  private void addDependency(PrintWriter writer, Iterable<PackageDependency> depends, String configuration) {
    for (PackageDependency depend : depends) {

      if(depend.getName().equals("R")) {
        // ignore
      } else if(CorePackages.isCorePackage(depend.getName())) {
        if(!CorePackages.DEFAULT_PACKAGES.contains(depend.getName()) &&
          !CorePackages.IGNORED_PACKAGES.contains(depend.getName())) {
          writer.println("  " + configuration + " \"org.renjin:" + depend.getName() + ":${renjinVersion}\"");
        }
      } else {
        String dependencyString = packageIndex.getDependencyString(depend.getName());
        if (dependencyString == null && !configuration.equals("testRuntime")) {
          throw new RuntimeException(id + " is missing dependency " + depend);
        }
        if (dependencyString != null) {
          writer.println("  " + configuration + " " + dependencyString);
        }
      }
    }
  }

}
