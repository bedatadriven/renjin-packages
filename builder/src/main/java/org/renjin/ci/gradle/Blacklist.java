package org.renjin.ci.gradle;

import org.renjin.ci.model.PackageVersionId;

import java.util.HashSet;
import java.util.Set;

public class Blacklist {

  private Set<String> unsupportedCran = new HashSet<>();

  public Blacklist() {
    unsupportedCran.add("openssl");
    unsupportedCran.add("gifski");
    unsupportedCran.add("jpeg");
    unsupportedCran.add("magick");
    unsupportedCran.add("rgl");

  }

  public boolean isCompilationDisabled(PackageVersionId id) {
    return id.getGroupId().equals("org.renjin.cran") && unsupportedCran.contains(id.getPackageName());
  }
}
