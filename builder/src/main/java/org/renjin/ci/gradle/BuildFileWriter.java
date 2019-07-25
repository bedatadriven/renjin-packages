package org.renjin.ci.gradle;

import org.renjin.ci.gradle.graph.PackageNode;

import java.io.File;
import java.io.PrintWriter;

public interface BuildFileWriter {
  void write(PackageNode node, File packageDir, PrintWriter writer);
}
