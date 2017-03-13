package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;

import java.io.IOException;


public class BlasDetection {

  public static String detectionScript(FilePath scriptOutput) {
    StringBuilder script = new StringBuilder();
    script.append("system(sprintf('sh -c \"lsof -p %d > ").append(scriptOutput.getRemote()).append("\"', Sys.getpid()))\n");

    return script.toString();
  }

  public static String findSystemBlas(FilePath scriptOutput) throws IOException, InterruptedException {
    String[] lines = scriptOutput.readToString().split("\\n");
    for (String line : lines) {
      if(line.contains("libopenblas")) {
        return "OpenBLAS";
      } if (line.contains("mkl")) {
        return "MKL";
      }
    }
    return "reference";
  }
}
