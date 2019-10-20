package org.renjin.ci.gradle.graph;

import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Maintains a queue of packages to build, ensuring that dependency
 * order is maintained.
 */
public class PackageGraph implements Serializable {


  private final Map<PackageId, PackageNode> nodes;

  public PackageGraph(Map<PackageId, PackageNode> nodes) {
    this.nodes = nodes;
  }

  public Collection<PackageNode> getNodes() {
    return nodes.values();
  }

  public int size() {
    return nodes.size();
  }


}