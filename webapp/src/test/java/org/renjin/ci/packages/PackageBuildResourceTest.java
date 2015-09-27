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
import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;

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
  
  
}
