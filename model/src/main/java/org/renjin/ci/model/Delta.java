package org.renjin.ci.model;

/**
 * Change in status between Renjin Versions
 */
public enum Delta {
  REGRESSION(-1),
  NO_CHANGE(0),
  IMPROVEMENT(+1);

  private final int code;

  Delta(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }


  public static Delta valueOf(int delta) {
    if(delta < 0) {
      return REGRESSION;
    } else if(delta > 0) {
      return IMPROVEMENT;
    } else {
      return NO_CHANGE;
    }
  }
}
