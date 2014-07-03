package org.renjin.ci.model;

public enum BuildOutcome {
  /**
   * The Build failed due to an error, such as OutOfMemoryException
   * or other potentially transient issue
   */
  ERROR,
  /**
   * The build failed
   */
  FAILURE,
  /**
   * The build succeeded: an artifact
   * was produced
   */
  SUCCESS,

  /**
   * The build timed out before completing
   */
  TIMEOUT,

  CANCELLED;
}
