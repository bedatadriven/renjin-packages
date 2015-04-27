package org.renjin.ci.model;

public class CompatibilityFlags {

  public static final int SEVERE_MASK = 0xFF;
  public static final int PARTIAL_MASK = 0xFF;
  
  public static final int NATIVE_COMPILATION_FAILURE = 0x1;
  public static final int TEST_FAILURES = 0x2;
  public static final int NO_TESTS = 0x4;
  
  public static final int BUILD_FAILURE = 0x200;
  public static final int NO_TESTS_PASSING = 0x402;


}
