package org.renjin.build;

import org.junit.Test;
import org.renjin.build.model.PackageDatabase;
import org.renjin.build.model.PackageVersion;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.tasks.Fixtures;
import org.renjin.build.tasks.ResolveDependenciesTask;

import java.io.IOException;

public class BuildResourceTest extends AbstractDatastoreTest {

  @Test
  public void testPom() throws IOException {
    PackageVersion mass = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));
    PackageDatabase.save(mass);

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);
    survey.setDescription(Fixtures.getSurveyPackageDescriptionSource());
    new ResolveDependenciesTask().resolveDependencies(survey);

    PackageDatabase.save(survey);

    BuildResource buildResource = new BuildResource();
    System.out.println(buildResource.getPom(surveyId.getGroupId(), surveyId.getPackageName(), surveyId.getSourceVersion(), "100"));


  }

}
