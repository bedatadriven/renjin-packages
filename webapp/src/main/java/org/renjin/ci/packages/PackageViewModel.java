package org.renjin.ci.packages;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.renjin.ci.model.*;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackageViewModel {

  private String groupId;
  private String name;
  private List<PackageVersion> versions;
  private List<PackageBuild> builds;

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

  public List<PackageBuild> getBuilds() {
    return builds;
  }

  public void setBuilds(List<PackageBuild> builds) {
    this.builds = builds;
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
