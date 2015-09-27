package org.renjin.ci.datastore;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores regression/progression history per PackageVersion
 */
@Entity
public class PackageVersionDelta {
  
  @Id
  private String packageVersion;

  @Index
  private Set<String> renjinVersions;
  
  private List<BuildDelta> builds;

  public PackageVersionDelta() {
  }

  public PackageVersionDelta(PackageVersionId packageVersionId, Collection<BuildDelta> builds) {
    this.packageVersion = packageVersionId.toString();
    this.builds = Lists.newArrayList(builds);
    this.renjinVersions = new HashSet<>();

    for (BuildDelta build : builds) {
      renjinVersions.add(build.getRenjinVersion());
    }
  }

  public String getPackageVersion() {
    return packageVersion;
  }

  public PackageVersionId getPackageVersionId() {
    return PackageVersionId.fromTriplet(packageVersion);
  }
  
  public List<BuildDelta> getBuilds() {
    return builds;
  }

  public Optional<BuildDelta> getBuild(String renjinVersion) {
    for (BuildDelta build : builds) {
      if(build.getRenjinVersion().equals(renjinVersion)) {
        return Optional.of(build);
      }
    }
    return Optional.absent();
  }


  public PackageId getPackageId() {
    return getPackageVersionId().getPackageId();
  }
}
