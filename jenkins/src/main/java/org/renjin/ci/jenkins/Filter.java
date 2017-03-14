package org.renjin.ci.jenkins;


import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import org.renjin.ci.jenkins.benchmark.Benchmark;

public class Filter implements Predicate<Benchmark> {
  private String includes;
  private String excludes[];
  private boolean noDependencies;

  public Filter(String includes, String excludes, boolean noDependencies) {
    this.includes = includes;
    this.excludes = Strings.nullToEmpty(excludes).split("\\s*,\\s*");
    this.noDependencies = noDependencies;
  }

  public boolean apply(Benchmark benchmark) {
    if(!apply(benchmark.getName())) {
      return false;
    }
    if(noDependencies && benchmark.hasDependencies()) {
      return false;
    }
    return true;
  }

  boolean apply(String name) {
    if(!Strings.isNullOrEmpty(includes) && !name.contains(includes)) {
      return false;
    }
    for (String exclude : excludes) {
      if(name.contains(exclude)) {
        return false;
      }
    }
    return true;
  }
}
