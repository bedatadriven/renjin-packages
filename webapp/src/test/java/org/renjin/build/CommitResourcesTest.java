package org.renjin.build;

import freemarker.template.TemplateException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class CommitResourcesTest extends ResourceTest {

  private CommitResources resource;

  @Before
  public void setUp() {
    this.resource = new CommitResources();
  }

  @Test
  public void testIndex() throws IOException, TemplateException {
    assertTemplateRenders(resource.getCommitIndex());
  }

  @Test
  public void compare() throws IOException, TemplateException {
    assertTemplateRenders(resource.getProgress("11296081328313eaddcd98aa26159d18748e6fdb", "0.7.0-RC5"));
  }

  @Test
  public void testHistory() throws IOException, TemplateException {
    assertTemplateRenders(resource.getTestHistory("11296081328313eaddcd98aa26159d18748e6fdb", 8096));
  }

}
