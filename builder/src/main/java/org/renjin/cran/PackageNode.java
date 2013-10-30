package org.renjin.cran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.renjin.repo.model.PackageDescription;

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
