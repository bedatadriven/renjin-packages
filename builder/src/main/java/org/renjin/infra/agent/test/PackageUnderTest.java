package org.renjin.infra.agent.test;

public class PackageUnderTest {
  private String groupId;
  private String name;
  private String version;

  public PackageUnderTest(String groupId, String name, String version) {
    this.groupId = groupId;
    this.name = name;
    this.version = version;
  }

  public PackageUnderTest(String gav) {
    String[] parts = gav.split(":");
    if(parts.length != 3) {
      throw new IllegalArgumentException("Expected groupId:artifactId:version");
    }
    this.groupId = parts[0];
    this.name = parts[1];
    this.version = parts[2];
  }

  public String getGroupId() {
    return groupId;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return name + ":" + version;
  }

  public String getPackageId() {
    return groupId + ":" + name;
  }

  public String getPackageVersionId() {
    return getPackageId() + ":" + getVersion();
  }
}
