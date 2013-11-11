package org.renjin.infra.agent.test;

import java.io.File;
import java.util.concurrent.Callable;

public class TestCase {
  private PackageUnderTest packageUnderTest;
  private File testFile;

  public TestCase(PackageUnderTest packageUnderTest, File testFile) {
    this.packageUnderTest = packageUnderTest;
    this.testFile = testFile;
  }

  public PackageUnderTest getPackageUnderTest() {
    return packageUnderTest;
  }

  public File getTestFile() {
    return testFile;
  }

  @Override
  public String toString() {
    return packageUnderTest + ": " + testFile.getName();
  }

}
