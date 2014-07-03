package org.renjin.ci.model;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CorePackages {
  private static final Set<String> CORE_PACKAGES =
    Sets.newHashSet(
      "stats", "stats4", "graphics", "grDevices",
      "utils", "methods", "datasets", "splines", "grid",
      "splines");

  public static final List<String> DEFAULT_PACKAGES =
    Lists.newArrayList("stats", "graphics", "grDevices",
      "utils", "datasets", "methods");


  public static boolean isCorePackage(String name) {
    return CORE_PACKAGES.contains(name);
  }
  
}
