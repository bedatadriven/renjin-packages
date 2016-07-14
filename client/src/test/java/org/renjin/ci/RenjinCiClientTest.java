package org.renjin.ci;

import org.junit.Test;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageVersionId;

import java.util.Arrays;
import java.util.List;

/**
 * Created by alex on 14-7-16.
 */
public class RenjinCiClientTest {

  @Test
  public void resolveRandomForest() {

    List<PackageVersionId> resolved = RenjinCiClient.resolveDependencies(Arrays.asList(new PackageDependency("randomForest (4.6-12)")));
    System.out.println(resolved);
  }
  
}