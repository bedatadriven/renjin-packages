package org.renjin.ci;

import org.junit.Ignore;
import org.junit.Test;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alex on 14-7-16.
 */
public class RenjinCiClientTest {

  @Ignore
  @Test
  public void resolveRandomForest() {

    List<PackageVersionId> resolved = RenjinCiClient.resolveDependencies(Arrays.asList(new PackageDependency("randomForest (4.6-12)")));
    System.out.println(resolved);
  }


  @Test
  public void test() throws IOException {
    String patchId = RenjinCiClient.getPatchedVersionId(new PackageVersionId("org.renjin.cran", "BiocGenerics", "0.18.0"));
    System.out.println(patchId);
  }
}