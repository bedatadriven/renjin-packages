package org.renjin.build.model;

/**
 *
 */
public enum BuildStage {

  /**
   * The package has been scheduled to be built, but is
   * waiting for dependencies to complete
   */
  WAITING,

  /**
   * The package's dependencies are built or resolved,
   * ready to build
   */
  READY,

  /**
   * The build has been leased to a worker for building
   */
  LEASED,

  /**
   * The build has completed
   */
  COMPLETED


}
