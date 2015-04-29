package org.renjin.ci.index;

import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.tasks.Fixtures;

import java.io.IOException;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PackageRegistrationTasksTest extends AbstractDatastoreTest {

  @Test
  public void testRegisterOrphan() throws IOException, ParseException {

    PackageRegistrationTasks task = new PackageRegistrationTasks();
    PackageVersion survey = task.register(new PackageVersionId("org.renjin.cran", "survey", "3.29-5"),
        Fixtures.getSurveyPackageDescriptionSource());

    assertThat(survey.getId(), equalTo("org.renjin.cran:survey:3.29-5"));
    assertThat(survey.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(survey.getPublicationDate(), equalTo(new LocalDateTime(2013,6,12, 17,40,8)));

  }

  @Test
  public void register() throws IOException, ParseException {

    PackageVersion mass2 = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));
    PackageDatabase.save(mass2).now();

    PackageRegistrationTasks task = new PackageRegistrationTasks();
    PackageVersion survey = task.register(new PackageVersionId("org.renjin.cran", "survey", "3.29-5"),
        Fixtures.getSurveyPackageDescriptionSource());

    assertThat(survey.getId(), equalTo("org.renjin.cran:survey:3.29-5"));
    assertThat(survey.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(survey.getPublicationDate(), equalTo(new LocalDateTime(2013,6,12, 17,40,8)));

  }
}
