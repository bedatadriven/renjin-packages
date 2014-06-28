package org.renjin.build.jenkins;

import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalURLFetchServiceTestConfig;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.renjin.build.PomBuilder;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.model.RenjinVersionId;
import org.renjin.build.tasks.Fixtures;
import org.renjin.build.tasks.QueuePackageBuildTask;

import java.io.IOException;

public class JenkinsClientTest {

  private LocalServiceTestConfig urlFetch = new LocalURLFetchServiceTestConfig();

  private LocalServiceTestHelper helper = new LocalServiceTestHelper(urlFetch);

  @Before
  public void setUp() throws Exception {
    helper.setUp();

  }

  @Test
  public void test() throws IOException {
    JenkinsClient client = new JenkinsClient();
    System.out.println(client.getJobs());
  }
//
//  @Test
//  public void testNewJob() throws Exception {
//
//    PackageVersionId packageVersionId = new PackageVersionId("org.renjin.cran:pps:0.94:0.7.0-RC7");
//
//    PackageBuild build = new PackageBuild(packageVersionId, 190);
//    build.setDependencies(Sets.<String>newHashSet());
//    build.setRenjinVersion(RenjinVersionId.RELEASE);
//
//    PomBuilder pomBuilder = new PomBuilder(build, Fixtures.getPpsDescription());
//
//    QueuePackageBuildTask task = new QueuePackageBuildTask();
//    task.launchJob(packageVersionId, pomBuilder);
//  }
}
