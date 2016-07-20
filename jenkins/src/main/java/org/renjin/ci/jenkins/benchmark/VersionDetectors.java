package org.renjin.ci.jenkins.benchmark;

import hudson.Launcher;
import hudson.Proc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VersionDetectors {
  
  public static String detectJavaVersion(Launcher launcher) throws IOException, InterruptedException {
    return parseJavaVersion(execute(launcher, "java", "-version"));
  }
  

  public static String detectGccVersion(Launcher launcher) throws IOException, InterruptedException {
    return parseGccVersion(execute(launcher, "gcc", "-v"));    
  }


  static String parseJavaVersion(String... lines) {
    String vendor = "Unknown";
    String version = "unknown";

    for (String line : lines) {
      if(line.contains("OpenJDK")) {
        vendor = "OpenJDK";
      } else if(line.contains("HotSpot")) {
        vendor = "Oracle";
      }
      if(line.contains("version \"")) {
        int startVersion = line.indexOf('"')+1;
        int endVersion = line.indexOf('"', startVersion);
        version = line.substring(startVersion, endVersion);
      }
    }
    return vendor + "-" + version;
  }


  private static String parseGccVersion(String[] lines) {
    for (String line : lines) {
      if(line.startsWith("gcc version")) {
        int paren = line.indexOf('(');
        return line.substring("gcc version".length(), paren).trim();
      }
    }
    return "unknown";
  }
  
  private static String[] execute(Launcher launcher, String... args) throws IOException, InterruptedException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).stdout(baos);

    Proc proc = launcher.launch(ps);
    int exitCode = proc.join();

    String output = new String(baos.toByteArray());

    if (exitCode != 0) {
      throw new RuntimeException("Failed to start JVM to find version: " + output);
    }

    return output.split("\n");
  }

  public static String findSystemBlas(Launcher launcher) throws IOException, InterruptedException {
    return parseSystemBlas(execute(launcher, "ldconfig", "-p"));
  }

  private static String parseSystemBlas(String[] output) {
    for (String line : output) {
      if(line.contains("libopenblas.so")) {
        return "OpenBLAS";
      } else if(line.contains("blas.so")) {
        return "reference";
      }
    }
    return "unknown";
  }
}
