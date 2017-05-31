package org.renjin.ci.jenkins.tools;

import hudson.FilePath;

import java.io.IOException;

/**
 * Archives test and build logs
 */
public interface LogArchiver {
  void archiveLog() throws IOException;

  void archiveTestOutput(String testName, FilePath outputFile) throws IOException;
}
