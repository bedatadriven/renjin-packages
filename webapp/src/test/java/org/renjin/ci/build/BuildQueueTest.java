package org.renjin.ci.build;

import junit.framework.TestCase;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.model.*;
import org.renjin.ci.tasks.Fixtures;
import org.renjin.ci.tasks.ResolveDependenciesTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class BuildQueueTest extends AbstractDatastoreTest {

  @Test
  public void testDownstreamEnqueue() throws IOException {

    RenjinVersionId release = new RenjinVersionId("0.7.0-RC7");

    PackageVersion mass = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));
    PackageVersion survival = new PackageVersion(new PackageVersionId("org.renjin.cran", "survival", "1.0"));

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);
    survey.setDescription(Fixtures.getSurveyPackageDescriptionSource());
    new ResolveDependenciesTask().resolveDependencies(survey);

    PackageStatus surveyStatus = new PackageStatus(surveyId, release);
    surveyStatus.setBuildStatus(BuildStatus.BLOCKED);
    surveyStatus.setBlockingDependenciesFrom(Arrays.asList(
        mass.getPackageVersionId(),
        survival.getPackageVersionId()));

    ofy().save().entities(mass, survival, survey, surveyStatus).now();

    /// MASS finishes, should be removed from list of packages blocking survey
    BuildQueue queue = new BuildQueue();
    queue.queueDownstream(mass.getPackageVersionId().toString(), 101);
    String massBuildId = mass.getPackageVersionId() + "-b101";

    PackageStatus surveyStatus2 = PackageDatabase.getStatus(surveyId, release);
    assertThat(surveyStatus2.getBlockingDependencies(), contains(survival.getId()));
    assertThat(surveyStatus2.getDependencies(), contains(massBuildId));
    assertThat(surveyStatus2.getBuildStatus(), equalTo(BuildStatus.BLOCKED));

    // NOW survival finishes, survey should be unblocked
    queue.queueDownstream(survival.getPackageVersionId().toString(), 45);
    String survivalBuildId = survival.getPackageVersionId() + "-b45";

    PackageStatus surveyStatus3 = PackageDatabase.getStatus(surveyId, release);
    assertThat(surveyStatus3.getBlockingDependencies(), Matchers.hasSize(0));
    assertThat(surveyStatus3.getDependencies(), containsInAnyOrder(massBuildId, survivalBuildId));
    assertThat(surveyStatus3.getBuildStatus(), equalTo(BuildStatus.READY));
  }
}
