package org.renjin.ci.gradle;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.renjin.ci.gradle.graph.PackageGraph;
import org.renjin.ci.gradle.graph.PackageNode;
import org.renjin.ci.gradle.graph.ReplacedPackageProvider;
import org.renjin.ci.model.CorePackages;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageDescription;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Sets a gradle project to build
 */
public class ProjectBuilder {

  private final File rootDir;
  private final String parentModule;
  private final PackageGraph graph;

  private ExecutorService executorService;
  private final Blacklist blacklist;
  private final ReplacedPackageProvider replacedPackages;

  private List<Future<?>> tasks = new ArrayList<>();

  public ProjectBuilder(ExecutorService executorService, File rootDir, String parentModule, PackageGraph graph, Blacklist blacklist,  ReplacedPackageProvider replacedPackages) {
    this.rootDir = rootDir;
    this.parentModule = parentModule;
    this.graph = graph;
    this.executorService = executorService;
    this.blacklist = blacklist;
    this.replacedPackages = replacedPackages;
  }

  public void setupDirectories() throws IOException, ExecutionException, InterruptedException {

    File subDir = new File(rootDir, parentModule);

    for (PackageNode node : graph.getNodes()) {
      if(!node.isProvided()) {
        File packageDir = new File(subDir, node.getId().getPackageName());
        PackageSetup task = new PackageSetup(blacklist, node, packageDir, this::writeBuildFile);
        tasks.add(executorService.submit(task));
      }
    }

    for (Future<?> task : tasks) {
      task.get();
    }

    updateSettingsFile();
  }

  private void writeBuildFile(PackageNode packageNode, File packageDir, PrintWriter writer) {


    File descriptionFile = new File(packageDir, "DESCRIPTION");
    PackageDescription description;
    try {
      description = PackageDescription.fromCharSource(Files.asCharSource(descriptionFile, Charsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Map<String, PackageNode> dependencyMap = packageNode.getDependencies()
      .stream()
      .collect(Collectors.toMap(d -> d.getId().getPackageName(), d -> d));


    writer.println("group = '" + packageNode.getId().getGroupId() + "'");
    writer.println();
    writer.println("apply plugin: 'org.renjin.package'");


    boolean needsCompilation = description.isNeedsCompilation() &&
      !blacklist.isCompilationDisabled(packageNode.getId());

    if(needsCompilation) {
      writer.println("apply plugin: 'org.renjin.native-sources'");
    }

    writer.println();
    writer.println("dependencies {");

    addDependency(writer, description.getDepends(), dependencyMap, "compile");
    addDependency(writer, description.getImports(), dependencyMap, "compile");
    if(needsCompilation) {
      addDependency(writer, description.getLinkingTo(), dependencyMap, "link");
    }
    addDependency(writer, description.getSuggests(), dependencyMap, "testRuntime");

    if(needsCompilation && hasCplusplusSources(packageDir)) {
      writer.println("  compile 'org.renjin:libstdcxx:4.7.4-b34'");
    }
    if(packageNode.getId().getPackageName().equals("testthat")) {
      writer.println("  compile 'org.renjin.cran:xml2:+'");
    }

    writer.println("}");
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

  private void addDependency(PrintWriter writer, Iterable<PackageDependency> depends, Map<String, PackageNode> dependencyMap, String configuration) {
    for (PackageDependency depend : depends) {

      if(CorePackages.isCorePackage(depend.getName())) {
        if(!CorePackages.DEFAULT_PACKAGES.contains(depend.getName()) &&
           !CorePackages.IGNORED_PACKAGES.contains(depend.getName())) {
          writer.println("  " + configuration + " \"org.renjin:" + depend.getName() + ":${renjinVersion}\"");
        }
      } else {
        PackageNode node = dependencyMap.get(depend.getName());
        if (node != null) {
          if (node.isProvided()) {
            writer.println("  " + configuration + " 'org.renjin.cran:" + depend.getName() + ":" + node.getBuildResult().getBuildVersion() + "'");
          } else {
            writer.println("  " + configuration + " project(':" + parentModule + ":" + depend.getName() + "')");
          }
        }
      }
    }
  }

  private void updateSettingsFile() throws IOException {
    File settingsFile = new File(rootDir, "settings.gradle");
    List<String> lines = Files.readLines(settingsFile, Charsets.UTF_8);

    StringBuilder updated = new StringBuilder();
    for (String line : lines) {
      if(!line.startsWith("include '" + parentModule + ":") && !line.startsWith("includeBuild '../renjin-replacements/")) {
        updated.append(line).append("\n");
      }
    }
    while(updated.length() > 0 && updated.charAt(updated.length() - 1) == '\n') {
      updated.setLength(updated.length() - 1);
    }
    updated.append("\n\n");

    replacedPackages.appendIncludeBuilds(updated);

    updated.append("\n\n");
    for (PackageNode node : graph.getNodes()) {
      if (!node.isProvided()) {
        updated.append("include '" + parentModule + ":" + node.getId().getPackageName()).append("'\n");
      }
    }

    Files.write(updated.toString(), settingsFile, Charsets.UTF_8);
  }
}
