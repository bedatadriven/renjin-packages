package org.renjin.ci.jenkins;


import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import org.renjin.ci.jenkins.benchmark.Benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Filter implements Predicate<Benchmark> {

  private static final Logger LOGGER = Logger.getLogger(Filter.class.getName());

  private String includes;
  private List<String> excludes;
  private boolean noDependencies;

  public Filter(String includes, String excludes, boolean noDependencies) {
    this.includes = includes;
    this.excludes = parseExcludes(excludes);
    this.noDependencies = noDependencies;
  }

  private static List<String> parseExcludes(String excludeList) {
    String[] patterns = Strings.nullToEmpty(excludeList).split("\\s*,\\s*");
    List<String> excludes = new ArrayList<String>();
    for (String pattern : patterns) {
      if(!Strings.isNullOrEmpty(pattern)) {
        excludes.add(pattern);
      }
    }
    return excludes;
  }

  public boolean apply(Benchmark benchmark) {
    if(!apply(benchmark.getName())) {
      return false;
    }
    if(noDependencies && benchmark.hasDependencies()) {
      LOGGER.info("Rejecting " + benchmark.getName() + " because it has dependencies...");
      return false;
    }
    return true;
  }

  boolean apply(String name) {
    if(!Strings.isNullOrEmpty(includes) && !name.contains(includes)) {
      LOGGER.info("Rejecting " + name + " because it does not match includes");
      return false;
    }
    for (String exclude : excludes) {
      if(name.contains(exclude)) {
        LOGGER.info("Rejecting " + name + " because it matches exclude '" + exclude + "'");
        return false;
      }
    }
    return true;
  }
}
