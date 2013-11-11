package org.renjin.infra.agent.test;

import com.google.common.collect.Lists;
import org.renjin.infra.agent.workspace.Workspace;

import java.io.File;
import java.util.List;

public class TestQueue {

  private final Workspace workspace;

  private List<PackageUnderTest> packages = Lists.newArrayList();
  private List<TestCase> cases = Lists.newArrayList();

  public TestQueue(Workspace workspace) {
    this.workspace = workspace;
  }

  public void addPackage(PackageUnderTest put) {
    packages.add(put);
    enumerateTests(put);
  }

  public void run() throws Exception {
    for(TestCase testCase : cases) {
      System.out.println(testCase.toString());
      new TestTask(workspace, testCase).call();
    }
  }

  private void enumerateTests(PackageUnderTest put) {
    File baseDir = new File(workspace.getPackagesDir(), put.getName() + "_" + put.getVersion());
    if(!baseDir.exists()) {
      throw new RuntimeException("Source directory doesn't exist: " + baseDir.getAbsolutePath());
    }
    File manDir = new File(baseDir, "man");

    for(File file : manDir.listFiles()) {
      if(file.getName().endsWith(".Rd")) {
        cases.add(new TestCase(put, file));
      }
    }
  }
}
