package org.renjin.ci.tasks;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.renjin.ci.model.PackageDescription;

import java.io.IOException;

public class Fixtures {

  public static PackageDescription getSurveyPackageDescription() throws IOException {

    return PackageDescription.fromString(
        getSurveyPackageDescriptionSource());
  }

  public static String getSurveyPackageDescriptionSource() throws IOException {
    return Resources.asCharSource(Resources.getResource(Fixtures.class, "survey-description.txt"),
        Charsets.UTF_8).read();
  }

  public static PackageDescription getPpsDescription() throws IOException {
    return PackageDescription.fromString(
        Resources.asCharSource(Resources.getResource(Fixtures.class, "pps-description.txt"),
            Charsets.UTF_8).read());
  }
}
