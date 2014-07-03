package org.renjin.ci.packages;

import freemarker.template.TemplateException;
import junit.framework.TestCase;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.model.*;
import org.renjin.ci.tasks.Fixtures;
import org.renjin.ci.tasks.ResolveDependenciesTask;

import java.io.IOException;
import java.util.Arrays;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Created by alex on 6/30/14.
 */
public class PackageResourceTest extends AbstractDatastoreTest {

  @Test
  public void test() throws IOException, TemplateException {

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);
    survey.setDescription(Fixtures.getSurveyPackageDescriptionSource());
    new ResolveDependenciesTask().resolveDependencies(survey);

    PackageStatus surveyStatus = new PackageStatus(surveyId, RenjinVersionId.RELEASE);
    surveyStatus.setBuildStatus(BuildStatus.BUILT);
    surveyStatus.setBuildNumber(101);


    ofy().save().entities(survey, surveyStatus).now();

    PackageResource resource = new PackageResource("org.renjin.cran", "survey");
    ResourceTest.assertTemplateRenders(resource.get());


  }
}
