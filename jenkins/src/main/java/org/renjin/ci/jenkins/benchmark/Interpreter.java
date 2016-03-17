package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;
import hudson.Launcher;

import java.io.IOException;

/**
 * Common interface to R Interpreters
 */
public abstract class Interpreter {
  abstract void ensureInstalled() throws IOException, InterruptedException;

  public abstract void execute(Launcher launcher, FilePath runScript) throws IOException, InterruptedException;
}
