package org.renjin.ci.packages;

import com.google.common.collect.Iterables;
import org.joda.time.LocalDate;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a set of packages and their dependencies to versions
 * released before a certain date.
 */
public class SnapshotResolution {

  private LocalDate latestDate;

  private Set<String> packageMap = new HashSet<>();
  private List<ResolvedDependency> resolution = new ArrayList<>();

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

    packageMap.add(packageName);

    if(latestVersion == null) {
      ResolvedDependency unresolved = new ResolvedDependency(packageName);
      resolution.add(unresolved);
    }

    PackageDescription description = latestVersion.getDescription().get();
    Iterable<PackageDependency> dependencies = Iterables.concat(
        description.getImports(),
        description.getDepends(),
        description.getLinkingTo());

    for (PackageDependency dependency : dependencies) {
      if(!CorePackages.isPartOfRenjin(dependency.getName())) {
        if (!packageMap.contains(dependency.getName())) {
          addPackage(dependency.getName());
        }
      }
    }

    resolution.add(new ResolvedDependency(latestVersion.getPackageVersionId()));
  }

  public ResolvedDependencySet build() {
    return new ResolvedDependencySet(resolution);
  }
}
