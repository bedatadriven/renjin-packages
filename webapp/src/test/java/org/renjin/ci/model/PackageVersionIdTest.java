package org.renjin.ci.model;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PackageVersionIdTest {

    
    @Test
    public void testIsNewer() {
        
        PackageVersionId existingVersion = PackageVersionId.fromTriplet("org.renjin.cran:lucid:1.0");
        PackageVersionId newVersion = PackageVersionId.fromTriplet("org.renjin.cran:lucid:1.2");
        
        assertTrue(newVersion.isNewer(existingVersion));
        
    }
}