package org.renjin.ci.gradle;

import org.junit.Test;
import org.renjin.ci.gradle.graph.*;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageGraphTest {

  @Test
  public void test() throws Exception {

    ReplacedPackageProvider replacedPackages = new ReplacedPackageProvider(new File("/home/alex/dev/renjin/replacements"));

    File rootDir = new File("/home/alex/dev/renjin/packages");

    ExecutorService executorService = Executors.newFixedThreadPool(12);

    DependencyCache dependencyCache = new DependencyCache(rootDir, "cran");

    PackageGraphBuilder builder = new PackageGraphBuilder(executorService, dependencyCache, replacedPackages, false, true);
    builder.add("org.renjin.cran:MASS:7.3-51.4", null);
    builder.add("org.renjin.cran:Matrix:1.2-17");
    builder.add("org.renjin.cran:ggplot2:3.1.1", null);
    builder.add("org.renjin.cran:testthat:2.1.1", null);
    builder.add("org.renjin.cran:data.table:1.12.2", null);
    builder.add("org.renjin.cran:xgboost:0.82.1", null);
    builder.add("org.renjin.cran:knitr:1.23", null);
    PackageGraph graph = builder.build();

    System.out.println("Package count: " + graph.getNodes().size());

    Blacklist blacklist = new Blacklist();

    ProjectBuilder projectBuilder = new ProjectBuilder(executorService, rootDir, "cran", graph, blacklist, replacedPackages);
    projectBuilder.setupDirectories();


  }
}
