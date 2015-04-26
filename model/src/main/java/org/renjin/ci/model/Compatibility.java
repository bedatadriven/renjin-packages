package org.renjin.ci.model;


public class Compatibility {

    /**
     * The package has not or could not yet be built, because of missing dependencies, or 
     * a problem with the build.
     */
    public static final int NOT_AVAILABLE = 0;

    /**
     * The package is available, but all tests are failing.
     */
    public static final int BROKEN = 1;

    /**
     * The package is available, but there are test failures.
     */
    public static final int PARTIAL = 2;


    /**
     * The package is available, and all test are passing.
     */
    public static final int HIGH = 3;
    
    
    public static final int TEST_FAILURES = 0x1;
    
    
}
