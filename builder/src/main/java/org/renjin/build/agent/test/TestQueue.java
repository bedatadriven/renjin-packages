package org.renjin.build.agent.test;

import com.google.common.collect.Lists;
import org.renjin.build.agent.build.PackageNode;
import org.renjin.build.agent.workspace.Workspace;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestQueue {

  private final Workspace workspace;

  private List<PackageNode> packages = Lists.newArrayList();
  private int numConcurrentTests;

  public TestQueue(Workspace workspace, Set<PackageNode> built) {
    this.workspace = workspace;
    this.packages = Lists.newArrayList(built);
  }

  public void setNumConcurrentTests(int numConcurrentTests) {
    this.numConcurrentTests = numConcurrentTests;
  }

  public void run() throws Exception {
    List<PackageTesterTask> tasks = Lists.newArrayList();
    for(PackageNode packageUnderTest : packages) {
      System.out.println(packageUnderTest.toString());
      tasks.add(new PackageTesterTask(workspace, packageUnderTest)) ;
    }
    ExecutorService executorService = Executors.newFixedThreadPool(numConcurrentTests);
    executorService.invokeAll(tasks);
    executorService.shutdown();
  }

}
