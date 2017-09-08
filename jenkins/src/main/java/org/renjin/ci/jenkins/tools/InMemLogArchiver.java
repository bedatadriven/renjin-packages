package org.renjin.ci.jenkins.tools;

import hudson.FilePath;

import java.io.IOException;

public class InMemLogArchiver implements LogArchiver {
  @Override
  public void archiveLog() throws IOException {
  }

  @Override
  public void archiveTestOutput(String testName, FilePath outputFile) throws IOException {

  }
}
