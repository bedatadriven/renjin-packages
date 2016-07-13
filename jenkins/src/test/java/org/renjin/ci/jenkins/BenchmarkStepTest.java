package org.renjin.ci.jenkins;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * Created by alex on 13-7-16.
 */
public class BenchmarkStepTest {

  @Ignore
  @Test
  public void test() {
    List<String> versions = BenchmarkStep.expandRenjinVersions("0.8,");
    System.out.println(versions);
  }
}