package org.renjin.ci.packages;

import com.googlecode.objectify.ObjectifyService;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.datastore.PackageVersionDescription;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.tasks.Fixtures;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.renjin.ci.datastore.PackageBuild.buildSucceeded;
import static org.renjin.ci.packages.PackageBuildResource.findProgression;
import static org.renjin.ci.packages.PackageBuildResource.findRegression;

public class PackageBuildResourceTest extends AbstractDatastoreTest {

  @Test
  public void testResultPage() throws IOException, TemplateException {
    PackageVersion mass = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);

    ObjectifyService.ofy().save().entity(
        new PackageVersionDescription(surveyId, Fixtures.getSurveyPackageDescriptionSource())).now();
    
    PackageBuild build15 = new PackageBuild(surveyId, 15);
    build15.setOutcome(BuildOutcome.TIMEOUT);
    build15.setRenjinVersion("0.7.0-RC7");
    build15.setStartTime(new Date().getTime());
    build15.setEndTime(new Date().getTime());

    PackageBuild build16 = new PackageBuild(surveyId, 16);
    build16.setOutcome(BuildOutcome.SUCCESS);
    build16.setRenjinVersion("0.7.0-RC7");
    build16.setStartTime(new Date().getTime());
    build16.setEndTime(new Date().getTime());

    ofy().save().entities(mass, survey, build15, build16).now();

    PackageBuildResource packageBuildResource = new PackageBuildResource(surveyId, 16);
    ResourceTest.assertTemplateRenders(packageBuildResource.get());
  }
  
  @Test
  public void deltasOnEmptySets() {
    Iterable<PackageBuild> builds = Collections.emptySet();
    assertThat(findProgression(builds, buildSucceeded()), nullValue());
    assertThat(findRegression(builds, buildSucceeded()), nullValue());
  }
  
  @Test
  public void deltasOnSingleItemSets() {
    assertThat(findProgression(asList(build(true)), buildSucceeded()), nullValue());
    assertThat(findRegression(asList(build(true)), buildSucceeded()), nullValue());
    assertThat(findProgression(asList(build(false)), buildSucceeded()), nullValue());
    assertThat(findRegression(asList(build(false)), buildSucceeded()), nullValue());
  }

  @Test
  public void simpleRegression() {
    PackageBuild succeeded = build(true);
    PackageBuild failed = build(false);
    assertThat(findRegression(asList(succeeded, failed), buildSucceeded()), is(failed));
  }
  
  @Test
  public void repeatedRegressions() {
    PackageBuild badBuild = build(false);

    
    // Lots of regressions along the way, but we only care about the last one.
    
    Iterable<PackageBuild> sequence = asList(build(false), build(true), build(false), build(true), build(true), badBuild);
    
    assertThat(findRegression(sequence, buildSucceeded()), is(badBuild));
  }
  
  @Test
  public void simpleProgression() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded), buildSucceeded()), is(succeeded));
  }


  @Test
  public void progressionFollowedByRegression() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded, build(false), build(false)), buildSucceeded()), is(succeeded));
  }

  @Test
  public void progressionFollowedByRegressionThenFixed() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded, build(false), build(false), build(true)), buildSucceeded()), is(succeeded));
  }


  private PackageBuild build(boolean success) {
    PackageBuild build = new PackageBuild();
    build.setOutcome(success ? BuildOutcome.SUCCESS : BuildOutcome.FAILURE);
    return build;
  }
  
}
