package org.renjin.ci.gradle;

import org.junit.Test;
import org.renjin.ci.gradle.graph.PackageGraph;
import org.renjin.ci.gradle.graph.PackageGraphBuilder;
import org.renjin.ci.gradle.graph.PackageNode;

import java.io.File;

public class PackageGraphTest {

  @Test
  public void test() throws Exception {
    PackageGraphBuilder builder = new PackageGraphBuilder(false, true);
    builder.add("org.renjin.cran:MASS:7.3-51.4", null);
    builder.add("org.renjin.cran:Matrix:1.2-17");
//    builder.add("org.renjin.cran:ggplot2:3.1.1", null);

    PackageGraph graph = builder.build();

    for (PackageNode node : graph.getNodes()) {
      System.out.println(node.getId() + " => " + node.getBuildResult());
    }

    ProjectBuilder projectBuilder = new ProjectBuilder(new File("/home/alex/dev/renjin-3.5"), "cran", graph);
    projectBuilder.setupDirectories();


  }
}
