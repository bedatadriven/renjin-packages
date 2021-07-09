package org.renjin.ci.packages;

import junit.framework.TestCase;
import org.junit.Test;
import org.renjin.ci.model.PackageVersionId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ContributionTest  {

    @Test
    public void test() {
        Contribution contribution = Contribution.fetchFromMaven(PackageVersionId.fromTriplet("se.alipsa:spreadsheets:1.3"));
        
        assertThat(contribution.getUrl(), equalTo("https://github.com/Alipsa/spreadsheets"));
        assertThat(contribution.getDescription(), notNullValue());
    }

}