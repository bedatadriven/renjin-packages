package org.renjin.ci.gradle;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.renjin.ci.gradle.graph.PackageGraph;
import org.renjin.ci.gradle.graph.PackageNode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Sets a gradle project to build
 */
public class ProjectBuilder {

  private final File rootDir;
  private final String parentModule;
  private final PackageGraph graph;

  private ExecutorService downloadService = Executors.newSingleThreadExecutor();

  private List<Future<?>> tasks = new ArrayList<>();

  public ProjectBuilder(File rootDir, String parentModule, PackageGraph graph) {
    this.rootDir = rootDir;
    this.parentModule = parentModule;
    this.graph = graph;
  }

  public void setupDirectories() throws IOException, ExecutionException, InterruptedException {

    File subDir = new File(rootDir, parentModule);

    for (PackageNode node : graph.getNodes()) {
      File packageDir = new File(subDir, node.getId().getPackageName());
      PackageSetup task = new PackageSetup(node, packageDir, this::writeBuildFile);

//      tasks.add(downloadService.submit(task));
      task.run();
    }

    for (Future<?> task : tasks) {
      task.get();
    }

    updateSettingsFile();
  }

  private void writeBuildFile(PackageNode packageNode, File file, PrintWriter writer) {

    writer.println("group = '" + packageNode.getId().getGroupId() + "'");
    writer.println();
    writer.println("apply from: '../../gradle/package.gradle'");
    writer.println();
    writer.println("dependencies {");
    writer.println("  compile project(':core')");
    writer.println("  compile project(':packages:stats')");
    writer.println("  compile project(':packages:utils')");
    writer.println("  compile project(':packages:methods')");
    writer.println("  compile project(':packages:graphics')");
    writer.println("  compile project(':packages:grDevices')");
    writer.println("  compile project(':packages:datasets')");
    writer.println("  compile project(':packages:grid')");

    for (PackageNode dependency : packageNode.getDependencies()) {
      writer.println("  compile project(':" + parentModule + ":" + dependency.getId().getPackageName() + "')");
    }
    writer.println("}");
  }

  private void updateSettingsFile() throws IOException {
    File settingsFile = new File(rootDir, "settings.gradle");
    List<String> lines = Files.readLines(settingsFile, Charsets.UTF_8);

    StringBuilder updated = new StringBuilder();
    for (String line : lines) {
      if(!line.startsWith("include '" + parentModule + ":")) {
        updated.append(line).append("\n");
      }
    }
    while(updated.charAt(updated.length() - 1) == '\n') {
      updated.setLength(updated.length() - 1);
    }
    updated.append("\n\n");
    for (PackageNode node : graph.getNodes()) {
      updated.append("include '" + parentModule + ":" + node.getId().getPackageName()).append("'\n");
    }

    Files.write(updated.toString(), settingsFile, Charsets.UTF_8);
  }
}
