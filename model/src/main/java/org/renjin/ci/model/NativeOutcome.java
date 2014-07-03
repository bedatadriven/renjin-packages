package org.renjin.ci.model;

/**
 * Created by alex on 7/1/14.
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
