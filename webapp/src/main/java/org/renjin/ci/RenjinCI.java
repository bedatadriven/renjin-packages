package org.renjin.ci;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import freemarker.template.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.renjin.ci.admin.AdminResources;
import org.renjin.ci.archive.SourceTasks;
import org.renjin.ci.benchmarks.BenchmarksResource;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.index.GitHubTasks;
import org.renjin.ci.index.IndexTasks;
import org.renjin.ci.index.PackageRegistrationTasks;
import org.renjin.ci.index.WebHooks;
import org.renjin.ci.packages.PackageListResource;
import org.renjin.ci.packages.PackageResource;
import org.renjin.ci.packages.RootResource;
import org.renjin.ci.pulls.PullRequestResources;
import org.renjin.ci.qa.QaResources;
import org.renjin.ci.releases.InstallResource;
import org.renjin.ci.releases.ReleasesResource;
import org.renjin.ci.repo.MavenRepository;
import org.renjin.ci.repo.apt.AptRepository;
import org.renjin.ci.repo.apt.KeyServer;
import org.renjin.ci.source.SourceResources;
import org.renjin.ci.stats.StatsResources;

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

    classes.add(ReleasesResource.class);
    classes.add(InstallResource.class);
    
    classes.add(RootResource.class);
    classes.add(PackageResource.class);
    classes.add(IndexTasks.class);
    classes.add(PackageRegistrationTasks.class);
    classes.add(PackageListResource.class);
    classes.add(GitHubTasks.class);
    classes.add(WebHooks.class);
    classes.add(QaResources.class);
    classes.add(BenchmarksResource.class);
    classes.add(SourceTasks.class);
    classes.add(SourceResources.class);
    
    classes.add(AdminResources.class);
    classes.add(StatsResources.class);
    
    classes.add(RobotsResource.class);

    classes.add(MavenRepository.class);
    classes.add(AptRepository.class);

    classes.add(KeyServer.class);

    classes.add(PullRequestResources.class);

    classes.add(SystemRequirementResource.class);

    classes.add(NoRobotsFeature.class);


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
