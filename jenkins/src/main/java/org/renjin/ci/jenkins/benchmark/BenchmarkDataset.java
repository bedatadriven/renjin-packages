package org.renjin.ci.jenkins.benchmark;

import java.net.URL;

/**
 * Describes a dataset required by a benchmark
 */
public class BenchmarkDataset {

  private final String fileName;
  private final URL url;
  private final String hash;

  public BenchmarkDataset(String fileName, URL url, String hash) {
    this.hash = hash;
    this.url = url;
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  public URL getUrl() {
    return url;
  }

  public String getHash() {
    return hash;
  }
}
