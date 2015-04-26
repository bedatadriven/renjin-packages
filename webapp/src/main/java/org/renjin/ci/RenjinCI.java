package org.renjin.ci;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import freemarker.template.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.renjin.ci.benchmarks.BenchmarksResource;
import org.renjin.ci.index.IndexTasks;
import org.renjin.ci.index.PackageIndexTasks;
import org.renjin.ci.index.PackageRegistrationTasks;
import org.renjin.ci.index.WebHooks;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.packages.PackageResource;
import org.renjin.ci.packages.PackageSearch;
import org.renjin.ci.packages.TestResources;
import org.renjin.ci.qa.QaResources;
import org.renjin.ci.releases.Releases;

import javax.ws.rs.core.Application;
import java.util.Map;
import java.util.Set;

public class RenjinCI extends Application {

  public RenjinCI() {

    PackageDatabase.init();

  }

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = Sets.newHashSet();

    classes.add(JacksonFeature.class);
    classes.add(FreemarkerMvcFeature.class);
    
    classes.add(PackageResource.class);

    classes.add(IndexTasks.class);
    classes.add(PackageRegistrationTasks.class);
    classes.add(PackageIndexTasks.class);
    classes.add(PackageSearch.class);
    classes.add(WebHooks.class);
    classes.add(Releases.class);
    classes.add(QaResources.class);
    classes.add(BenchmarksResource.class);
    classes.add(TestResources.class);

    return classes;
  }

  @Override
  public Map<String, Object> getProperties() {
    Map<String, Object> properties = Maps.newHashMap();
    properties.put(MvcFeature.TEMPLATE_OBJECT_FACTORY + ".freemarker", templateConfiguration());
    return properties;
  }

  private Configuration templateConfiguration() {
    Configuration configuration = new Configuration();
    configuration.setClassForTemplateLoading(RenjinCI.class, "/");

    return configuration;
  }
}
