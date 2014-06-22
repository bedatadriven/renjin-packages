package org.renjin.build.fetch;

import org.junit.Test;

import java.net.URISyntaxException;

public class ResolveDependencyVersionsTaskTest {

  @Test
  public void test() throws URISyntaxException {

    ResolveDependencyVersionsTask task = new ResolveDependencyVersionsTask();
    task.resolve("org.renjin.cran:ALS");
  }
}
