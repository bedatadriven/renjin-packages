package org.renjin.build;

import freemarker.template.TemplateException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class BuildResourcesTest extends ResourceTest {

  private BuildResources resource;

  @Before
  public void setUp() {
    resource = new BuildResources();
  }

  @Test
  public void testGetBuildSummary() throws Exception {
    assertTemplateRenders(resource.getBuildSummary(84, null));
  }

  @Test
  public void testGetRelativeBuildSummary() throws IOException, TemplateException {
    assertTemplateRenders(resource.getBuildSummary(84, 18));

  }

  @Test
  public void testGetBuildResult() throws Exception {
    assertTemplateRenders(resource.getBuildResult(151, "org.renjin.cran", "actuar", "1.1-6").getIndex());
  }

  @Test
  public void testGetBuildResultWithTestHistory() throws Exception {
    assertTemplateRenders(resource.getBuildResult(64, "org.renjin.cran", "energy", "1.6.0").getIndex());
  }

  @Test
  public void retries() throws Exception {
    assertTemplateRenders(resource.getBuildResult(64, "org.renjin.cran", "boot", "1.3-9").getIndex());
  }
}
