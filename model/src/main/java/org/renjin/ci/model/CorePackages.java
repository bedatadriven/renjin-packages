package org.renjin.ci.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

public class CorePackages {
  public static final Set<String> CORE_PACKAGES =
    Sets.newHashSet(
      "base", "stats", "stats4", "graphics", "grDevices",
      "utils", "methods", "datasets", "splines", "grid",
      "splines", "parallel");

  public static final List<String> DEFAULT_PACKAGES =
    Lists.newArrayList("stats", "graphics", "grDevices",
      "utils", "datasets", "methods");


  public static boolean isCorePackage(String name) {
    return CORE_PACKAGES.contains(name);
  }

  public static boolean isPartOfRenjin(String packageName) {
    return packageName.equals("R") || isCorePackage(packageName);
  }
}
