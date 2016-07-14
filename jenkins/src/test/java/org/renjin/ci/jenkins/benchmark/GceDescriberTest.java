package org.renjin.ci.jenkins.benchmark;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Created by alex on 14-7-16.
 */
public class GceDescriberTest {
  
  @Test
  public void test() {
    assertFalse(GceDescriber.isGoogleComputeEngine());
  }

}