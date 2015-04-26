package org.renjin.ci.model;


public enum PackageSupport {

    /**
     * This package has not yet been built.
     */
    UNKNOWN(0),
    
    /**
     * There was an error building this package
     */
    ERROR(1),

    /**
     * There are tests failing, or there are no tests for this package
     */
    WARNING(2),

    /**
     * All tests are passing for this package
     */
    OK(3);
    
    private int level;

    PackageSupport(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
    
    public PackageSupport valueOf(int level) {
        switch (level) {
            case 0:
                return UNKNOWN;
            case 1:
                return ERROR;
            case 2: 
                return WARNING;
            case 3:
                return OK;
        }
        throw new IllegalArgumentException();
    }
}
