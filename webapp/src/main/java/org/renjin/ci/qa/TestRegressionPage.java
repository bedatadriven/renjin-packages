package org.renjin.ci.qa;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.List;
import java.util.Map;

/**
 * Page model showing all test regressions
 */
public class TestRegressionPage {

  public class Build {
    private PackageBuildId id;
    private RenjinVersionId renjinVersion;

    public Build(PackageBuildId id, RenjinVersionId renjinVersion) {
      this.id = id;
      this.renjinVersion = renjinVersion;
    }

    public Build(PackageVersionDelta delta, BuildDelta buildDelta) {
      this.id = new PackageBuildId(delta.getPackageVersionId(), buildDelta.getBuildNumber());
      this.renjinVersion = buildDelta.getRenjinVersionId();
    }

    public PackageBuildId getId() {
      return id;
    }

    public RenjinVersionId getRenjinVersion() {
      return renjinVersion;
    }
  }
  
  public class TestRegression { 
    private final String name;
    private Build brokenBuild;
    
    public TestRegression(String name, Build brokenBuild) {
      this.name = name;
      this.brokenBuild = brokenBuild;
    }
    
    public String getTestName() {
      return name;
    }

    public Build getBrokenBuild() {
      return brokenBuild;
    }
  }
  
  public class PackageResult {
    private final PackageId id;
    private List<TestRegression> regressions = Lists.newArrayList();
    
    public PackageResult(PackageId id) {
      this.id = id;
    }

    public void add(TestRegression regression) {
      regressions.add(regression);
    }

    public PackageId getId() {
      return id;
    }

    public List<TestRegression> getRegressions() {
      return regressions;
    }
  }

  private final Map<PackageId, PackageResult> packages = Maps.newHashMap();


  public TestRegressionPage(Iterable<PackageVersionDelta> deltas) {
    for (PackageVersionDelta delta : deltas) {
      for (BuildDelta buildDelta : delta.getBuilds()) {
        Build brokenBuild = new Build(delta, buildDelta);
        for (String testName : buildDelta.getTestRegressions()) {
          forPackage(delta.getPackageId()).add(new TestRegression(testName, brokenBuild));
        }
      }
    }
  }

  private PackageResult forPackage(PackageId packageId) {
    PackageResult packageResult = packages.get(packageId);
    if(packageResult == null) {
      packageResult = new PackageResult(packageId);
      packages.put(packageId, packageResult);
    }
    return packageResult;
  }
  
  public List<PackageResult> getPackages() {
    return Lists.newArrayList(packages.values());
  }
  
}
