package org.renjin.ci.gradle;

import org.renjin.ci.model.PackageVersionId;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageIndex {

  private List<PackageVersionId> toBuild = new ArrayList<>();

  /**
   * Maps the simple package name to the gradle dependency string.
   */
  private Map<String, String> packageNameMap = new HashMap<>();

  private Blacklist blacklist;

  public PackageIndex(File packageRoot) throws IOException {

    blacklist = new Blacklist(packageRoot);

    try(BufferedReader reader = new BufferedReader(new FileReader(new File(packageRoot, "packages.list")))) {
      String line;
      while( (line = reader.readLine()) != null) {
        if(line.endsWith("*")) {
          String triplet = line.substring(0, line.length() - 1);
          PackageVersionId id = PackageVersionId.fromTriplet(triplet);
          packageNameMap.put(id.getPackageName(), "'" + triplet + "'");

        } else {
          PackageVersionId id = PackageVersionId.fromTriplet(line);
          toBuild.add(id);
          packageNameMap.put(id.getPackageName(), "project(':cran:" + id.getPackageName() + "')");
        }
      }
    }
  }

  public List<PackageVersionId> getToBuild() {
    return toBuild;
  }

  /**
   * Finds the Gradle dependency string for a given package name.
   */
  public String getDependencyString(String packageName) {
    return packageNameMap.get(packageName);
  }

  public Blacklist getBlacklist() {
    return blacklist;
  }
}
