package org.renjin.ci.model;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertThat;


public class PackageDescriptionTest {

  @Test
  public void testCollate() throws IOException {

    InputStream in = getClass().getResourceAsStream("/testthat");
    if(in == null) {
      throw new AssertionError("Can't find test file");
    }
    
    PackageDescription description = PackageDescription.fromInputStream(in);

    List<String> sourceFiles = description.getCollate().get();
    
    assertThat(sourceFiles, Matchers.hasItems( "auto-test.r", "colour-text.r", "compare.r", "context.r",
    "describe.r", "evaluate-promise.r", "expect-that.r",
    "expectation.r", "expectations-equality.R",
    "expectations-matches.R", "expectations-old.R", "expectations.r",
    "make-expectation.r", "mock.r", "reporter.r", "reporter-check.R",
    "reporter-list.r", "reporter-minimal.r", "reporter-multi.r",
    "reporter-rstudio.R", "reporter-silent.r", "reporter-stop.r",
    "reporter-summary.r", "reporter-tap.r", "reporter-teamcity.r",
    "reporter-zzz.r", "test-example.R", "test-files.r",
    "test-package.r", "test-results.r", "test-that.r", "traceback.r",
    "utils.r", "watcher.r"));


  }
  
}