package org.renjin.ci.workflow.graph;


public enum NodeState  {
  /**
   * This package depends on one or more packages that must still be built
   */
  BLOCKED,

  /**
   * This package's dependencies have been resolved and is ready to build
   */
  READY,

  /**
   * One or more of this package's dependencies failed to build 
   */
  ORPHANED,


  /**
   * This package has been built and either succeeded or failed.
   */
  BUILT;
}
