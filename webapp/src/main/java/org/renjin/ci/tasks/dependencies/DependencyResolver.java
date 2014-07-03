package org.renjin.ci.tasks.dependencies;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.joda.time.LocalDate;
import org.renjin.ci.model.*;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Resolves unqualified, unversioned dependency
 */
public class DependencyResolver {


  private String groupId;

  /**
   * The publication date of the package version for which we
   * are resolving dependencies
   */
  private LocalDate publicationDate;

  public DependencyResolver(String groupId, LocalDate publicationDate) {
    this.groupId = groupId;
    this.publicationDate = publicationDate;
  }

  public DependencyResolver(PackageVersion packageVersion) {
    this(packageVersion.getGroupId(), new LocalDate(packageVersion.getPublicationDate()));
  }

  public boolean isPartOfRenjin(PackageDescription.PackageDependency dependency) {
    return isPartOfRenjin(dependency.getName());
  }

  public boolean isPartOfRenjin(String packageName) {
    return packageName.equals("R") || CorePackages.isCorePackage(packageName);
  }

  public Optional<PackageVersionId> resolveVersion(PackageDescription.PackageDependency dependency) {

    // 1. Find all PackageVersions in our database that match the dependency
    List<PackageVersion> packageVersions =
        PackageDatabase.queryPackageVersions(groupId, dependency.getName());

    // Apply the version range, if applied by the DESCRIPTION file
    Predicate<ArtifactVersion> expectedRange = versionRangePredicate(dependency);

    // Try to find the highest matching version, within the expected range,
    // that was published before this package

    Set<PackageVersion> matchingVersions = Sets.newHashSet();
    for(PackageVersion packageVersion : packageVersions) {
      if(expectedRange.apply(packageVersion.getVersion())) {
        matchingVersions.add(packageVersion);
      }
    }

    if(matchingVersions.isEmpty()) {
      return Optional.absent();
    } else {
      return Optional.of(Collections.max(matchingVersions).getPackageVersionId());
    }
  }

  private Predicate<ArtifactVersion> versionRangePredicate(PackageDescription.PackageDependency dependency) {
    if(dependency.getVersion() != null) {
      return Predicates.<ArtifactVersion>equalTo(new DefaultArtifactVersion(dependency.getVersion()));

    } else if(dependency.getVersionRange() != null) {
      try {
        final VersionRange range = VersionRange.createFromVersionSpec(dependency.getVersionRange());
        return new Predicate<ArtifactVersion>() {
          @Override
          public boolean apply(@Nullable ArtifactVersion input) {
            return range.containsVersion(input);
          }
        };
      } catch (InvalidVersionSpecificationException e) {
        throw new RuntimeException("Failed to parse version specification: " + dependency.getVersionRange());
      }
    }
    return Predicates.alwaysTrue();
  }

}
