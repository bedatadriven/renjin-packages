package org.renjin.ci.build;

import freemarker.template.TemplateException;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.index.dependencies.DependencyResolver;
import org.renjin.ci.model.*;
import org.renjin.ci.tasks.Fixtures;

import java.io.IOException;
import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class BuildResourceTest extends AbstractDatastoreTest {

  @Test
  public void testResultPage() throws IOException, TemplateException {
    PackageVersion mass = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);
    survey.setDescription(Fixtures.getSurveyPackageDescriptionSource());

    DependencyResolver.update(survey);

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

    BuildResource buildResource = new BuildResource("org.renjin.cran", "survey", "3.29-5", 16);
    ResourceTest.assertTemplateRenders(buildResource.get());
  }

}
