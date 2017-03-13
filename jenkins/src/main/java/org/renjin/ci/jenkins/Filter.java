package org.renjin.ci.jenkins;


import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import org.renjin.ci.jenkins.benchmark.Benchmark;

public class Filter implements Predicate<Benchmark> {
  private String includes;
  private String excludes;
  private boolean noDependencies;

  public Filter(String includes, String excludes, boolean noDependencies) {
    this.includes = includes;
    this.excludes = excludes;
    this.noDependencies = noDependencies;
  }

  public boolean apply(Benchmark benchmark) {
    if(!Strings.isNullOrEmpty(includes) && !benchmark.getName().contains(includes)) {
      return false;
    }
    if(!Strings.isNullOrEmpty(excludes) && benchmark.getName().contains(excludes)) {
      return false;
    }
    if(noDependencies && benchmark.hasDependencies()) {
      return false;
    }
    return true;
  }
}
