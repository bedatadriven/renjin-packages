package org.renjin.infra.agent.test;

import com.google.common.collect.Lists;
import org.renjin.infra.agent.workspace.Workspace;

import java.util.List;

public class TestQueue {

  private final Workspace workspace;

  private List<PackageUnderTest> packages = Lists.newArrayList();

  public TestQueue(Workspace workspace) {
    this.workspace = workspace;
  }

  public void addPackage(PackageUnderTest put) {
    packages.add(put);
  }

  public void run() throws Exception {
    for(PackageUnderTest put : packages) {
      System.out.println(put.toString());
      new PackageTesterHarness(workspace, put).run();
    }
  }

}
