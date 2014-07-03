package org.renjin.ci.tasks;

import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

public class ResolveDependenciesTaskTest extends AbstractDatastoreTest {

  @Test
  public void resolve() throws IOException, ParseException {

    // setup our catalog
    PackageVersion mass = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "14.0"));

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);
    survey.setDescription(Fixtures.getSurveyPackageDescriptionSource());

    PackageDatabase.save(mass, survey).now();

    // Try to resolve dependencies
    ResolveDependenciesTask task = new ResolveDependenciesTask();
    task.resolve(surveyId.toString());

    // verify that we've resolved the package
    PackageVersion updatedSurvey = PackageDatabase.getPackageVersion(surveyId).get();
    assertThat(updatedSurvey.isCompileDependenciesResolved(), equalTo(true));
    assertThat(updatedSurvey.getDependencyIdSet(), hasItem(mass.getPackageVersionId()));
  }

}
