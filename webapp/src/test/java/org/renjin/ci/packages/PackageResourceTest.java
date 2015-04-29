package org.renjin.ci.packages;

import freemarker.template.TemplateException;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.tasks.Fixtures;

import java.io.IOException;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class PackageResourceTest extends AbstractDatastoreTest {

  @Test
  public void test() throws IOException, TemplateException {

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-5");
    PackageVersion survey = new PackageVersion(surveyId);

    ofy().save().entities(survey).now();

    PackageResource resource = new PackageResource("org.renjin.cran", "survey");
    ResourceTest.assertTemplateRenders(resource.get());


  }
}
