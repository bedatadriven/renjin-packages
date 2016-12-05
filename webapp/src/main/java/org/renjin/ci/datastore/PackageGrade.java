package org.renjin.ci.datastore;

public class PackageGrade {

    /**
     * Compilation succeeds and no tests fail
     */
    public static final char A = 'A';

    /**
     * Majority of tests pass
     */
    public static final char B = 'B';


    /**
     * At least one test passes
     */
    public static final char C = 'C';


    /**
     * No tests pass.
     */
    public static final char F = 'F';
}
