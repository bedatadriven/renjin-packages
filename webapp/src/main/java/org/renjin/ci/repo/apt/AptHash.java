package org.renjin.ci.repo.apt;

public class AptHash {
  private String name;
  private String hash;

  public AptHash(String name, String hash) {
    this.name = name;
    this.hash = hash;
  }

  public String getName() {
    return name;
  }

  public String getHash() {
    return hash;
  }
}
