package org.renjin.ci.packages;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.renjin.ci.model.*;
import org.renjin.ci.packages.results.TestRun;

import javax.annotation.Nullable;
import java.util.*;

public class PackageViewModel {

  private String groupId;
  private String name;
  private List<PackageVersion> versions;
  private List<PackageBuild> builds;
  private Map<Long, TestRun> testRuns = new HashMap<>();
  private Set<RenjinVersionId> renjinVersions = new HashSet<>();

  public PackageViewModel(String groupId, String name) {
    this.groupId = groupId;
    this.name = name;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getName() {
    return name;
  }

  public List<PackageVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<PackageVersion> versions) {
    this.versions = versions;
  }

  public void setTestRuns(Iterable<PackageTestResult> results) {
    for (PackageTestResult result : results) {
      TestRun run = testRuns.get(result.getTestRunNumber());
      if(run == null) {
        run = new TestRun(result.getTestRunNumber(), result.getRenjinVersionId(), result.getPackageVersionId(), result.getPackageBuildNumber());
        testRuns.put(result.getTestRunNumber(), run);
        renjinVersions.add(result.getRenjinVersionId());
      }
      run.add(result);
    }
  }

  public Collection<TestRun> getTestRuns() {
    return testRuns.values();
  }
  
  public Iterable<PackageBuild> getBuilds() {
    return builds;
  }

  public void setBuilds(List<PackageBuild> builds) {
    this.builds = builds;
    for(PackageBuild build : builds) {
      renjinVersions.add(build.getRenjinVersionId());
    }
  }

  
  
  public Set<RenjinVersionId> getRenjinVersions() {
    return renjinVersions;
  }

  public PackageVersion getLatestVersion() {
    return Collections.max(versions, Ordering.natural().onResultOf(new Function<PackageVersion, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(PackageVersion input) {
        return input.getVersion();
      };
    }));
    
  }
}
