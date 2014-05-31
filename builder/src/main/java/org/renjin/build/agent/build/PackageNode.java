package org.renjin.build.agent.build;

import java.util.List;

import com.google.common.collect.Lists;

public class PackageNode {

  private String groupId;
  private String packageName;
  private String packageVersion;
  private List<PackageEdge> edges = Lists.newArrayList();

  public PackageNode(String groupId, String packageName, String packageVersion) {
    this.groupId = groupId;
    this.packageName = packageName;
    this.packageVersion = packageVersion;
  }

  public String getId() {
    return getPackageId() + ":" + packageVersion;
  }

  public String getVersion() {
    return packageVersion;
  }

  public String getPackageId() {
    return groupId + ":" + packageName;
  }

  public String getName() {
	  return packageName;
	}

  @Override
  public String toString() {
    return getName();
  }

  public String getGroupId() {
    return groupId;
  }

  public List<PackageEdge> getEdges() {
    return edges;
  }
}
