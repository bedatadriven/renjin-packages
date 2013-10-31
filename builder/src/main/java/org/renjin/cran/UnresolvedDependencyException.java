package org.renjin.cran;


public class UnresolvedDependencyException extends Exception {
  private String packageName;

  public UnresolvedDependencyException(String packageName) {
    this.packageName = packageName;
  }

  public String getPackageName() {
    return packageName;
  }
}
