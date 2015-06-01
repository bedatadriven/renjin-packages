package org.renjin.ci.test;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.repository.DependencyResolver;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DependencyResolverTest {
  
  @Test
  public void resolveSurveyPackage() throws Exception {

    PackageVersionId surveyId = new PackageVersionId("org.renjin.cran", "survey", "3.29-4");
    PackageBuildId buildId = new PackageBuildId(surveyId, 244);
    
    DependencyResolver resolver = new DependencyResolver();
    List<URL> urls = new ArrayList<>();
    urls.addAll(resolver.resolveRenjin(new RenjinVersionId("0.7.1523")));
    urls.addAll(resolver.resolvePackage(buildId));

    System.out.println(Joiner.on("\n").join(urls));
  }

}