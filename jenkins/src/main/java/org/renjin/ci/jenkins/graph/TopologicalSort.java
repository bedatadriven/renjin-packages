package org.renjin.ci.jenkins.graph;

import java.util.ArrayList;
import java.util.List;

public class TopologicalSort {

  private static final int TEMPORARY_MARK = 1;
  private static final int PERMANENT_MARK = 2;

  private List<PackageNode> sorted = new ArrayList<PackageNode>();

  private TopologicalSort(PackageGraph graph) {

    PackageNode unmarked;
    while((unmarked = findNextUnmarked(graph)) != null) {
      visit(unmarked);
    }
  }

  public static List<PackageNode> sort(PackageGraph graph) {
    TopologicalSort sort = new TopologicalSort(graph);
    return sort.sorted;
  }

  private void visit(PackageNode node) {
    if(node.mark == PERMANENT_MARK) {
      return;
    }
    if(node.mark == TEMPORARY_MARK) {
      throw new IllegalStateException("Not a DAG");
    }
    node.mark = TEMPORARY_MARK;
    for (PackageNode packageNode : node.getDependencies()) {
      visit(packageNode);
    }
    node.mark = PERMANENT_MARK;
    sorted.add(node);
  }

  private PackageNode findNextUnmarked(PackageGraph graph) {
    for (PackageNode packageNode : graph.getNodes()) {
      if(packageNode.mark == 0) {
        return packageNode;
      }
    }
    return null;
  }
}
