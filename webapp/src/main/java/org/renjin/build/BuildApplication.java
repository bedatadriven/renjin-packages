package org.renjin.build;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.googlecode.objectify.ObjectifyService;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.renjin.build.model.*;
import org.renjin.build.tasks.RegisterPackageVersionTask;
import org.renjin.build.util.TupleObjectWrapper;

import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class BuildApplication extends Application {


  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = Sets.newHashSet();
    classes.add(RootResources.class);
    classes.add(JacksonFeature.class);
    classes.add(FreemarkerMvcFeature.class);
    classes.add(RegisterPackageVersionTask.class);
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
    configuration.setObjectWrapper(new TupleObjectWrapper());
    configuration.setClassForTemplateLoading(BuildApplication.class, "/");


    return configuration;
  }


}
