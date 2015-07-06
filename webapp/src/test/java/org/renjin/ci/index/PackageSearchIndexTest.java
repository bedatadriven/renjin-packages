package org.renjin.ci.index;

import freemarker.template.TemplateException;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ResourceTest;
import org.renjin.ci.packages.PackageListResource;

import java.io.IOException;

import static org.junit.Assert.*;

public class PackageSearchIndexTest extends AbstractDatastoreTest {

  @Test
  public void testEmptyQuery() throws IOException, TemplateException {

    PackageListResource resource = new PackageListResource();
    ResourceTest.assertTemplateRenders(resource.search(null));
    ResourceTest.assertTemplateRenders(resource.search(""));

  }
  
}