package org.renjin.ci.qa;

import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * Created by alex on 26-9-17.
 */
@Ignore
public class TestRegressionResourceTest {

  @Test
  public void test() throws IOException {

    GHCompare diff = GitHub.connectAnonymously().
        getRepository("bedatadriven/renjin")
        .getCompare("552a85d0cf1cccf9f6f046012a67ec4c7e39dd69", "0a86ff3379476c24cb3e3c7a901c5e92f57e5376");

    System.out.println(diff);
  }

}