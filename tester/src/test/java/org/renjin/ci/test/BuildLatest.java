package org.renjin.ci.test;

import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import javax.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by alex on 24-6-15.
 */
public class BuildLatest {
  
  public static void main(String args[]) throws Exception {
    TestRunner testRunner = new TestRunner(new RenjinVersionId("0.7.1534"));

    List<PackageVersionId> packages = RenjinCiClient.queryPackageList("latest", new HashMap<>());
    for (PackageVersionId packageVersionId : packages) {
      try {
        PackageBuildId buildId = RenjinCiClient.queryLastSuccessfulBuild(packageVersionId);
        System.out.println(packageVersionId + ": " + buildId);
        try {
          testRunner.testPackage(packageVersionId);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } catch (NotFoundException e) {
        System.out.println(packageVersionId + ": not built");
      }
      
    }
  }
}
