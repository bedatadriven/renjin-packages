package org.renjin.ci.jenkins.benchmark;

import com.google.common.collect.Lists;
import hudson.FilePath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Identifies a single benchmark
 */
public class Benchmark {
  private String name;
  private FilePath filePath;

  public Benchmark(String name, FilePath filePath) {
    this.name = name;
    this.filePath = filePath;
  }

  public String getName() {
    return name;
  }
  
  public FilePath getDirectory() {
    return filePath;
  }
  
  public String getLocalName() {
    int lastSlash = name.lastIndexOf('/');
    if(lastSlash == -1) {
      return name;
    } else {
      return name.substring(lastSlash+1);
    }
  }
  
  public FilePath getDescriptorPath() {
    return filePath.child("BENCHMARK.dcf");
  }
  
  public FilePath getScript() {
    return filePath.child(getLocalName() + ".R");
  }

  public List<BenchmarkDataset> readDatasets() throws IOException, InterruptedException {
    BufferedReader in = new BufferedReader(new InputStreamReader(getDescriptorPath().read()));
    String line;
    
    List<String> files = Lists.newArrayList();
    List<URL> sources = Lists.newArrayList();
    List<String> hash = Lists.newArrayList();
    
    while((line=in.readLine())!=null) {
      if(!line.trim().isEmpty()) {
        String[] keyValue = line.trim().split(":", 2);
        if (keyValue.length != 2) {
          throw new DcfFormatException("Malformed line: " + line);
        }
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();

        if (key.equals("File")) {
          files.add(value);
        } else if(key.equals("Source")) {
          sources.add(new URL(value));
        } else if(key.equals("Hash")) {
          hash.add(value);
        }
      }
    }

    List<BenchmarkDataset> datasets = Lists.newArrayList();
    for (int i = 0; i < files.size(); i++) {
      datasets.add(new BenchmarkDataset(files.get(i), sources.get(i), hash.get(i)));
    }
    return datasets;
  }
  
}
