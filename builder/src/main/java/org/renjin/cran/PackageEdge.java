package org.renjin.cran;


public class PackageEdge {
  private final String type;
  private PackageNode dependency;

  public PackageEdge(PackageNode dependency, String type) {
    this.dependency = dependency;
    this.type = type;
  }

  public PackageNode getDependency() {
    return dependency;
  }

  public String getType() {
    return type;
  }
}
