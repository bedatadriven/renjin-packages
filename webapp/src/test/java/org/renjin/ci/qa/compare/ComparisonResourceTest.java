package org.renjin.ci.qa.compare;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.model.RenjinVersionId;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ComparisonResourceTest extends AbstractDatastoreTest {

  @Test
  public void testReport() throws IOException, TemplateException {

    RenjinVersionId from = new RenjinVersionId("1.0");
    RenjinVersionId to = new RenjinVersionId("2.0");

    ComparisonResource resource = new ComparisonResource(from, to);
    Viewable viewable = resource.compareReleases();

    Map<String, Object> object = new HashMap<>();
    object.put("model", viewable.getModel());

    Configuration templateConfig = new Configuration();
    templateConfig.setClassForTemplateLoading(ComparisonResource.class, "/");
    templateConfig.getTemplate(viewable.getTemplateName()).process(object, new PrintWriter(System.out));
  }

}