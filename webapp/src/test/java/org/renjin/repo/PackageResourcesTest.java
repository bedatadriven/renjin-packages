package org.renjin.repo;


import com.google.common.collect.Maps;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PackageResourcesTest {

  private static EntityManager em;

  @BeforeClass
  public static void setup() {
    em = PersistenceUtil.createEntityManager();
  }

  @Test
  public void testIndex() throws IOException, TemplateException {

    BuildResultDAO dao = new BuildResultDAO(em);

    Configuration config = new Configuration();
    config.setClassForTemplateLoading(PackageResources.class, "/");

    Map<String, Object> model = Maps.newHashMap();
    model.put("buildResults", dao.queryResults(13));
    
    Template template = config.getTemplate("/index.ftl");
    template.process(model, new PrintWriter(System.out));
  }
}
