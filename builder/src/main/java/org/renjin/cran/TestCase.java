package org.renjin.cran;


import java.io.File;

public class TestCase {

  private final String packageVersionId;
  private final File workingDir;
  private final String source;

  public TestCase(String packageVersionId, File workingDir, String source) {
    this.packageVersionId = packageVersionId;
    this.workingDir = workingDir;
    this.source = source;
  }

  public String getPackageVersionId() {
    return packageVersionId;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public String getSource() {
    return source;
  }
}
