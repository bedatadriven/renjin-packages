package org.renjin.ci.workflow.graph;


public interface TransitionListener {
  
  void onTransition(PackageNode node, NodeState oldState);
  
}
