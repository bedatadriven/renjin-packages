package org.renjin.ci.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PackageVersionIdTest {

    
    @Test
    public void testIsNewer() {
        
        PackageVersionId existingVersion = PackageVersionId.fromTriplet("org.renjin.cran:lucid:1.0");
        PackageVersionId newVersion = PackageVersionId.fromTriplet("org.renjin.cran:lucid:1.2");
        
        assertTrue(newVersion.isNewer(existingVersion));
        
    }

    @Test
    public void versionSeparators() {
        PackageVersionId x = PackageVersionId.fromTriplet("org.renjin.cran:modeltools:0.3.4");
        PackageVersionId y = PackageVersionId.fromTriplet("org.renjin.cran:modeltools:0.3-40");

        assertTrue(y.isNewer(x));
    }

    @Test
    public void versionCompareTest() {
        assertThat(PackageVersionId.compareVersions("0.0", "0.0"),  equalTo( 0));
        assertThat(PackageVersionId.compareVersions("1.0", "0.0"),  equalTo(+1));
        assertThat(PackageVersionId.compareVersions("1.0", "10.0"), equalTo(-1));

        assertThat(PackageVersionId.compareVersions("1.0", "1.0.4"), equalTo(-1));
    }
}