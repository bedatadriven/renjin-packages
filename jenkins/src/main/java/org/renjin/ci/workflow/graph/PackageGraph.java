package org.renjin.ci.workflow.graph;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.renjin.ci.model.PackageVersionId;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Maintains a queue of packages to build, ensuring that dependency
 * order is maintained.
 */
public class PackageGraph implements Serializable {


  private final Map<PackageVersionId, PackageNode> nodes;

  public PackageGraph(Map<PackageVersionId, PackageNode> nodes) {
    this.nodes = nodes;
  }

  public Collection<PackageNode> getNodes() {
    return nodes.values();
  }

  @Whitelisted
  public int size() {
    return nodes.size();
  }


  @Whitelisted
  public BuildQueue newBuildQueue() {
    return new BuildQueue(this);
  }
}
