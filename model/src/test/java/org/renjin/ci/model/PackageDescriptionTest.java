package org.renjin.ci.model;

import org.hamcrest.Matchers;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.renjin.ci.model.PackageDescription.parseTimeFromPackaged;


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

  @Test
  public void testPackageTimeParse() {
    assertThat(parseTimeFromPackaged("Mon Nov 27 19:54:13 2006; warnes"),
        equalTo(new LocalDateTime(2006, 11, 27, 19, 54, 13)));
    assertThat(parseTimeFromPackaged("Sat Apr  7 09:42:10 2007; warnes"),
        equalTo(new LocalDateTime(2007, 4, 7, 9, 42, 10)));

    assertThat(parseTimeFromPackaged("Wed Aug  8 06:58:05 2007; warnes"),
        equalTo(new LocalDateTime(2007, 8, 8, 6, 58, 5)));

  }
}