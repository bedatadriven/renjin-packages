package org.renjin.ci.model;

/**
 * Describes the outcome of the native code compilation step 
 */
public enum NativeOutcome {

  NA,

  /**
   * The build failed
   */
  FAILURE,
  /**
   * The build succeeded: an artifact
   * was produced
   */
  SUCCESS,
}
