package org.renjin.infra.agent.test;

import org.renjin.infra.agent.workspace.Workspace;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

public class TestTask implements Callable<TestResult> {
  private Workspace workspace;
  private TestCase testCase;

  public TestTask(Workspace workspace, TestCase testCase) {
    this.workspace = workspace;
    this.testCase = testCase;
  }


  @Override
  public TestResult call() throws Exception {


    Process java = new ProcessBuilder("java",
      "-cp",
      buildClassPath(),
      TestExecutor.class.getName(),
      testCase.getPackageUnderTest().getName(),
      testCase.getTestFile().getAbsolutePath())
      .inheritIO()
      .start();

    int exitCode = java.waitFor();

    System.out.println(exitCode);

    return new TestResult();
  }

  private String buildClassPath() {
    StringBuilder classPath = new StringBuilder();

    // add our own dependencies to the path
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URL[] urls = ((URLClassLoader)cl).getURLs();
    for(URL url: urls) {
      if(classPath.length() > 0) {
        classPath.append(File.pathSeparator);
      }
      classPath.append(url.getFile());
    }

    // renjin dependencies
    if(classPath.length() > 0) {
      classPath.append(File.pathSeparator);
    }
    try {
      classPath.append(workspace.getDependencyResolver()
        .resolveClassPath("org.renjin:renjin-cli:" + workspace.getRenjinVersion()));
    } catch (Exception e) {
      throw new RuntimeException("Could not resolve renjin", e);
    }

    return classPath.toString();
  }
}
