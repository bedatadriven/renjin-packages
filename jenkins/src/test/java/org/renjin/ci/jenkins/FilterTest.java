package org.renjin.ci.jenkins;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterTest {

  @Test
  public void commaDelimitedExclude() {
    Filter filter = new Filter(null, "mutation, acs3yr", false);

    assertTrue(filter.apply("foobar"));
    assertFalse(filter.apply("bioinformatics/mutation"));
    assertFalse(filter.apply("survey/acs3yr"));
  }

}