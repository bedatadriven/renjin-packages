package org.renjin.build;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.glassfish.jersey.server.mvc.Viewable;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class ResourceTest {


  protected final void assertTemplateRenders(Viewable viewable) throws IOException, TemplateException {
    String templateName = viewable.getTemplateName();
    if(!templateName.startsWith("/")) {
      throw new AssertionError("Expected template name starting with '/', got: " + templateName);
    }
    templateName = templateName.substring(1);

    Configuration templateConfig = new Configuration();
    templateConfig.setClassForTemplateLoading(RootResources.class, "/");
    Template template = templateConfig.getTemplate(templateName);
    template.process(viewable.getModel(), new PrintWriter(System.out));
  }
}
