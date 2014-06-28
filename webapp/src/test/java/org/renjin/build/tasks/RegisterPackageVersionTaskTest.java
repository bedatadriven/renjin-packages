package org.renjin.build.tasks;

import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.renjin.build.AbstractDatastoreTest;
import org.renjin.build.model.*;

import java.io.IOException;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

public class RegisterPackageVersionTaskTest extends AbstractDatastoreTest {

  @Test
  public void testRegisterOrphan() throws IOException, ParseException {

    RegisterPackageVersionTask task = new RegisterPackageVersionTask();
    PackageVersion survey = task.register(new PackageVersionId("org.renjin.cran", "survey", "3.29-5"),
        Fixtures.getSurveyPackageDescriptionSource());

    assertThat(survey.getId(), equalTo("org.renjin.cran:survey:3.29-5"));
    assertThat(survey.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(survey.getPublicationDate(), equalTo(new LocalDateTime(2013,6,12, 17,40,8)));

  }

  @Test
  public void register() throws IOException, ParseException {

    PackageVersion mass2 = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));
    PackageDatabase.save(mass2);

    RegisterPackageVersionTask task = new RegisterPackageVersionTask();
    PackageVersion survey = task.register(new PackageVersionId("org.renjin.cran", "survey", "3.29-5"),
        Fixtures.getSurveyPackageDescriptionSource());

    assertThat(survey.getId(), equalTo("org.renjin.cran:survey:3.29-5"));
    assertThat(survey.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(survey.getPublicationDate(), equalTo(new LocalDateTime(2013,6,12, 17,40,8)));

    PackageStatus status = PackageDatabase.getStatus(survey.getPackageVersionId(), RenjinVersionId.RELEASE);
    assertThat(status.getBuildStatus(), equalTo(BuildStatus.BLOCKED));
    assertThat(status.getBlockingDependencies(), hasItem(mass2.getId()));
  }
}