package org.renjin.repo;

import org.junit.Before;
import org.junit.Test;

public class BuildResourcesTest extends ResourceTest {

  private BuildResources resource;

  @Before
  public void setUp() {
    resource = new BuildResources();
  }

  @Test
  public void testGetBuildSummary() throws Exception {
    assertTemplateRenders(resource.getBuildSummary(64));
  }

  @Test
  public void testGetBuildResult() throws Exception {
    assertTemplateRenders(resource.getBuildResult(64, "org.renjin.cran", "scidb", "1.1-0").getIndex());
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
