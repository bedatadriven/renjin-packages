package org.renjin.ci.index.dependencies;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.joda.time.LocalDate;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves unqualified, unversioned dependency
 */
public class DependencyResolver {

  private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

  /**
   * The group id to use for dependencies
   */
  private String groupId = "org.renjin.cran";

  /**
   * The publication date of the package version for which we
   * are resolving dependencies
   */
  private LocalDate publicationDate;

  public DependencyResolver() {
  }

  public DependencyResolver basedOnPublicationDateOf(LocalDate publicationDate) {
    this.publicationDate = publicationDate;
    return this;
  }

  public DependencyResolver basedOnPublicationDateFrom(PackageDescription description) {
    try {
      this.publicationDate = description.getPublicationDate().toLocalDate();
    } catch (ParseException e) {
      throw new IllegalArgumentException("date: " + e.getMessage(), e);
    }
    return this;
  }

  public boolean isPartOfRenjin(PackageDescription.PackageDependency dependency) {
    return CorePackages.isPartOfRenjin(dependency.getName());
  }

  public Optional<PackageVersionId> resolveVersion(PackageDescription.PackageDependency dependency) {

    // 1. Find all PackageVersions in our database that match the dependency
    List<PackageVersion> packageVersions =
        PackageDatabase.getPackageVersions(groupId, dependency.getName());

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

  public DependencySet resolveAll(PackageDescription description) {

    // Keep track of whether we are able to resolve dependencies
    DependencySet set = new DependencySet();
    set.setCompileDependenciesResolved(true);

    // Iterate over explicitly declared dependencies
    Iterable<PackageDescription.PackageDependency> declared =
        Iterables.concat(description.getImports(), description.getDepends());

    for(PackageDescription.PackageDependency dependency : declared) {
      if(!isPartOfRenjin(dependency)) {
        Optional<PackageVersionId> dependencyId = resolveVersion(dependency);
        if(dependencyId.isPresent()) {

          LOGGER.log(Level.INFO, "Resolved " + dependency + " to " + dependencyId.get());
          set.add(dependencyId.get());

        } else {
          LOGGER.log(Level.WARNING, "Could not resolve dependency " + dependency);
          set.setCompileDependenciesResolved(false);
        }
      }
    }

    return set;
  }

  public static void update(PackageVersion packageVersion) {

    PackageDescription description = packageVersion.parseDescription();
    DependencySet dependencySet = new DependencyResolver()
        .resolveAll(description);

    packageVersion.setDependencies(dependencySet);

  }
}
