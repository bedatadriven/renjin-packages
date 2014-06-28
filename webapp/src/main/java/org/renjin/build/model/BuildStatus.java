package org.renjin.build.model;

public enum BuildStatus {

  /**
   * We are missing the sources for one more dependencies
   */
  ORPHANED,

  /**
   * The build of this package is waiting for a dependency to build
   */
  BLOCKED,

  /**
   * This package is ready to build: all upstream dependencies are built
   */
  READY,

  /**
   * This package has been successfully built and deployed
   */
  BUILT,

  /**
   * We tried to build this package, but the process failed
   */
  FAILED
}