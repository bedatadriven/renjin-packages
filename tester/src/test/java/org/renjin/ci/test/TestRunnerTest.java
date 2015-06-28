package org.renjin.ci.test;

import org.junit.Ignore;
import org.junit.Test;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

public class TestRunnerTest {

  @Ignore
  @Test
  public void test() throws Exception {
    
    TestRunner testRunner = new TestRunner(new RenjinVersionId("0.7.1534"));
    testRunner.testPackage(new PackageVersionId("org.renjin.cran", "aspect", "1.0-3"));
    
  }
  
}