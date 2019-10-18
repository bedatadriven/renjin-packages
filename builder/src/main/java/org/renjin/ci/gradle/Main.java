package org.renjin.ci.gradle;

import org.renjin.ci.gradle.graph.DependencyCache;
import org.renjin.ci.gradle.graph.PackageGraph;
import org.renjin.ci.gradle.graph.PackageGraphBuilder;
import org.renjin.ci.gradle.graph.ReplacedPackageProvider;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {


  public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

    File universeRoot = new File(args[0]);

    System.out.println("Universe root: " + universeRoot.getAbsolutePath());

    ReplacedPackageProvider replacedPackages = new ReplacedPackageProvider(new File(universeRoot, "replacements"));

    File packageRootDir = new File(universeRoot, "packages");

    ExecutorService executorService = Executors.newFixedThreadPool(12);

    DependencyCache dependencyCache = new DependencyCache(packageRootDir, "cran");

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

    ProjectBuilder projectBuilder = new ProjectBuilder(executorService, packageRootDir, "cran", graph, blacklist, replacedPackages);
    projectBuilder.setupDirectories();

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
  }
}
