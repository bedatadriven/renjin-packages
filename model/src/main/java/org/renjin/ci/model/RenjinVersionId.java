package org.renjin.ci.model;

public class RenjinVersionId {

  public static final RenjinVersionId RELEASE = new RenjinVersionId("0.7.0-RC7");

  private final String version;

  public RenjinVersionId(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RenjinVersionId that = (RenjinVersionId) o;

    if (!version.equals(that.version)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }
}
