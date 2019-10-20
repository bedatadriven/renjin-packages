package org.renjin.ci.gradle;

import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Blacklist {

  private Set<String> set = new HashSet<>();

  public Blacklist(File packageRootDirectory) throws IOException {

    File blackListFile = new File(packageRootDirectory, "packages.blacklist");
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(blackListFile), StandardCharsets.UTF_8))) {
      String line;
      while((line = reader.readLine()) != null) {
        line = line.replaceAll("#.*$", "").trim();
        if(!line.isEmpty()) {
          set.add(line);
        }
      }
    }
  }

  public boolean isBlacklisted(PackageVersionId pvid) {
    return isBlacklisted(pvid.getPackageId());
  }

  public boolean isBlacklisted(PackageId id) {
    return id.getGroupId().equals(PackageId.CRAN_GROUP) && set.contains(id.getPackageName());
  }

  public boolean isBlacklisted(String name) {
    return set.contains(name);
  }

  public boolean isCompilationDisabled(PackageVersionId id) {
    return id.getGroupId().equals("org.renjin.cran") && set.contains(id.getPackageName());
  }
}
