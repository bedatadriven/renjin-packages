package org.renjin.ci.jenkins.tools;

import hudson.FilePath;

import java.io.IOException;

public class NullLogArchiver implements LogArchiver {
  @Override
  public void archiveLog() throws IOException {
  }

  @Override
  public void archiveTestOutput(String testName, FilePath outputFile) throws IOException {

  }
}
