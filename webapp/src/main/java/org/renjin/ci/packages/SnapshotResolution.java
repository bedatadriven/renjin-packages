package org.renjin.ci.packages;

import com.google.common.collect.Iterables;
import org.joda.time.LocalDate;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a set of packages and their dependencies to versions
 * released before a certain date.
 */
public class SnapshotResolution {

  private LocalDate latestDate;

  private Map<String, PackageVersion> packageMap = new HashMap<>();
  private List<PackageVersionId> resolution = new ArrayList<>();

  public SnapshotResolution(LocalDate latestDate) {
    this.latestDate = latestDate;
  }

  public void addPackage(String packageName) {
    List<PackageVersion> versions = PackageDatabase.getPackageVersions(new PackageId(PackageId.CRAN_GROUP, packageName));
    PackageVersion latestVersion = null;
    for (PackageVersion version : versions) {
      if(version.getLocalPublicationDate().isBefore(latestDate)) {
        if(latestVersion == null || version.getPackageVersionId().isNewer(latestVersion.getPackageVersionId())) {
          latestVersion = version;
        }
      }
    }
    if(latestVersion == null) {
      throw new IllegalArgumentException(String.format(
          "Could not find any package versions for %s published before %s", packageName, latestDate));

    }

    packageMap.put(packageName, latestVersion);

    PackageDescription description = latestVersion.getDescription().get();
    Iterable<PackageDependency> dependencies = Iterables.concat(
        description.getImports(),
        description.getDepends(),
        description.getLinkingTo());

    for (PackageDependency dependency : dependencies) {
      if(!CorePackages.isPartOfRenjin(dependency.getName())) {
        if (!packageMap.containsKey(dependency.getName())) {
          addPackage(dependency.getName());
        }
      }
    }

    resolution.add(latestVersion.getPackageVersionId());
  }

  public ResolvedDependencySet build() {

    List<ResolvedDependency> list = new ArrayList<>();
    for (PackageVersionId packageVersionId : resolution) {
      list.add(new ResolvedDependency(packageVersionId));
    }
    return new ResolvedDependencySet(list);
  }
}
