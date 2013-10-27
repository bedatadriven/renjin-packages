package org.renjin.repo;

import freemarker.template.TemplateException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CommitResourcesTest extends ResourceTest {

  private CommitResources resource;

  @Before
  public void setUp() {
    this.resource = new CommitResources();
  }

  @Test
  public void testTestHistory() throws IOException, TemplateException {
    assertTemplateRenders(resource.getTestHistory("412d8b64b20eef7cdab7fec380d0bc1bbb679edc", 7994));
  }
}
