package org.renjin.ci.source;

import org.junit.Test;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class JavaRedirectTest {

  @Test
  public void testFindRenjinFile() throws IOException {
    URL url = JavaRedirects.findFile("bedatadriven/renjin", "6089bb33fd70e56e4f15bfbf645814c25b968ca8", "SEXP.java");
    System.out.println(url);
  }

  @Test
  public void testFindPackageFile() throws IOException {
    URL url = JavaRedirects.findPackageSource(new PackageVersionId("org.renjin.cran", "lazyeval", "0.2.0"), "lazy.c");
    System.out.println(url);
  }

  @Test
  public void testPackageNameFromMethod()  {
    PackageId packageId = JavaRedirects.packageIdFromMethodName("org.renjin.cran.lazyeval.lazy__.make_lazy_dots");
    assertThat(packageId.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(packageId.getPackageName(), equalTo("lazyeval"));
  }

}