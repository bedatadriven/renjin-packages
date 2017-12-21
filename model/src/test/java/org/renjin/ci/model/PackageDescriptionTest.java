package org.renjin.ci.model;

import com.google.api.client.util.Lists;
import org.hamcrest.Matchers;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.renjin.ci.model.PackageDescription.parseTimeFromPackaged;


public class PackageDescriptionTest {

  @Test
  public void testCollate() throws IOException {

    PackageDescription description = fromResource("testthat");

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

  private PackageDescription fromResource(final String name) throws IOException {
    InputStream in = getClass().getResourceAsStream("/" + name);
    if(in == null) {
      throw new AssertionError("Can't find test file");
    }

    return PackageDescription.fromInputStream(in);
  }

  @Test
  public void testPackageTimeParse() {
    assertThat(parseTimeFromPackaged("Mon Nov 27 19:54:13 2006; warnes"),
        equalTo(new LocalDateTime(2006, 11, 27, 19, 54, 13)));
    assertThat(parseTimeFromPackaged("Sat Apr  7 09:42:10 2007; warnes"),
        equalTo(new LocalDateTime(2007, 4, 7, 9, 42, 10)));

    assertThat(parseTimeFromPackaged("Wed Aug  8 06:58:05 2007; warnes"),
        equalTo(new LocalDateTime(2007, 8, 8, 6, 58, 5)));

    assertThat(parseTimeFromPackaged("2017-06-07 08:34:30 UTC; dalex"),
        equalTo(new LocalDateTime(2017, 6, 7, 8, 34, 30)));

  }

  @Test
  public void parsePublicationDate() {

    assertThat(PackageDescription.parsePublicationDate("2017-06-08 04:43:11 UTC"),
        equalTo(new LocalDateTime(2017, 6, 8, 4, 43, 11)));
  }

  @Test
  public void parseNewReleaseDate() throws IOException, ParseException {

    PackageDescription mapproj = fromResource("mapproj");

    // 2017-06-08 04:43:11 UTC
    assertThat(mapproj.findReleaseDate(), equalTo(new LocalDateTime(2017, 6, 8, 4, 43, 11)));

  }

  @Test
  public void whitespaceInDependencies() throws IOException {
    PackageDescription description = fromResource("factominer");
    List<PackageDependency> imports = Lists.newArrayList(description.getImports());

    assertThat(imports.get(0).getName(), equalTo("car"));

  }
}