package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;
import org.renjin.ci.model.PackageDescription.PackageDependency;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.collect.Iterables.concat;


public class DependencyResolution {

  private static final Logger LOGGER = Logger.getLogger(DependencyResolution.class.getName());

  private PackageVersion packageVersion;

  public DependencyResolution(PackageVersion packageVersion) {

    this.packageVersion = packageVersion;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResolvedDependencySet resolve() {


    // Extract the declared dependencies from the DESCRIPTION file
    Map<String, PackageDependency> declared = enumerateDeclared();

    LOGGER.info("Declared: " + declared);


    // Then fetch a list of all candidate PackageVersions
    Map<String, Iterable<PackageVersion>> candidateVersions = new HashMap<>();
    
    for (String packageName : declared.keySet()) {
      if (!CorePackages.isCorePackage(packageName)) {
        candidateVersions.put(packageName, queryCandidateVersions(packageName));
      }
    }

    // Now find candidates that meet both declared criteria and availability of builds
    ResolvedDependencySet result = new ResolvedDependencySet();

    for (String packageName : declared.keySet()) {
      ResolvedDependency selected = select(declared.get(packageName), candidateVersions.get(packageName));
      result.getDependencies().add(selected);
    }
    
    LOGGER.info("Resolution: " + result.getDependencies());

    return result;
  }

  /**
   * @return a mapping from package names to a declared VersionRange (provided in the DESCRIPTION file)
   */
  private Map<String, PackageDependency> enumerateDeclared() {

    Map<String, PackageDependency> map = new HashMap<>();

    PackageDescription description = packageVersion.loadDescription();
    Iterable<PackageDependency> declared = concat(
        description.getImports(),
        description.getDepends());

    for (PackageDependency dependency : declared) {
      if(!CorePackages.isCorePackage(dependency.getName()) &&
          !CorePackages.IGNORED_PACKAGES.contains(dependency.getName())) {

        if(map.containsKey(dependency.getName())) {
          LOGGER.log(Level.WARNING, String.format("Dependency '%s' is declared multiple times in %s",
              dependency.getName(), packageVersion.getId()));
        } else {

          map.put(dependency.getName(), dependency);

        }
      }
    }
    return map;
  }
  
  private QueryResultIterable<Package> queryCandidatePackages(String packageName) {
    return ObjectifyService.ofy()
        .load()
        .type(Package.class)
        .filter("name", packageName).iterable();
  }

  private QueryResultIterable<PackageVersion> queryCandidateVersions(String packageName) {

    // Initiate a query for all the available SOURCE versions: this will
    // run asynchronously until we request the first result.
    return ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .filter("packageName", packageName).iterable();
  }


  /**
   * Selects a dependency version based on any explicit criteria in the DESCRIPTION file as well
   * as taking into account publication dates.
   */
  private ResolvedDependency select(PackageDependency declared, 
                                    Iterable<PackageVersion> candidates) {

    
    List<PackageVersion> candidateList = Lists.newArrayList(candidates);

    // Sort in descending order, so we can look from most recent to the oldest
    Collections.sort(candidateList, PackageVersion.orderByVersion().reverse());

    // Create a predicate based on the explicit version range specified in the DESCRIPTION file
    Predicate<PackageVersion> versionRange = versionConstraint(declared);

    Predicate<PackageVersion> publishedBefore = publishedBeforeConstraint();

    LOGGER.info(String.format("Resolving dependency %s satisfying %s from among: %s",
        declared, versionRange, candidateList));

    // FIRST: try to match a package that was PUBLISHED BEFORE this package
    // We can't know for sure which version the package author tested against
    // but we DO know that it could not have been a package from the future!
    for (PackageVersion version : candidateList) {
      if(publishedBefore.apply(version) && versionRange.apply(version)) {
        return resolution(version);
      }
    }
    
    // FALLBACK: if we don't have a package that fulfills both the published-before
    // criteria and the version range criteria, (possibly because our archive is incomplete)
    // then search from oldest to newest to find the dependency version closest in 
    // time to this package.
    Collections.reverse(candidateList);    

    for (PackageVersion version : candidateList) {
      if(versionRange.apply(version)) {
        return resolution(version);
      }
    }
    
    // No beans...
    return new ResolvedDependency(declared.getName());
  }

  private ResolvedDependency resolution(PackageVersion version) {
    
    // Check to see if this package is replaced by a Renjin-specific package
    Package packageEntity = PackageDatabase.getPackageOf(version.getPackageVersionId()).get();
    if(packageEntity.isReplaced()) {
      ResolvedDependency dependency = new ResolvedDependency(version.getPackageVersionId());
      dependency.setReplacementVersion(packageEntity.getLatestReplacementVersion());
      return dependency;
    }
    
    if(version.hasSuccessfulBuild()) {
      return new ResolvedDependency(version.getLastSuccessfulBuildId(), BuildOutcome.SUCCESS);
    } 
    
    // If there has been a failed build attempt, then return this information so we can avoid
    // attempting to build it again, unless explicitly requested.
    if(version.hasBuild()) {
      PackageBuild build = PackageDatabase.getBuild(version.getLastBuildId()).safe();
      if (build.getOutcome() == BuildOutcome.FAILURE) {
        return new ResolvedDependency(version.getLastBuildId(), BuildOutcome.FAILURE);
      }
    }
      
    return new ResolvedDependency(version.getPackageVersionId());
  }

  /**
   * Create a predicate for selecting candidates based on the explicit version range specified by the 
   * DESCRIPTION file, if there is one.
   */
  private Predicate<PackageVersion> versionConstraint(PackageDependency dependency) {
    if(!Strings.isNullOrEmpty(dependency.getVersionSpec())) {
      try {
        final VersionRange range = VersionRange.createFromVersionSpec(dependency.getVersionSpec());
        return new Predicate<PackageVersion>() {
          @Override
          public boolean apply(PackageVersion input) {
            return range.containsVersion(input.getVersion());
          }
        };
      } catch (InvalidVersionSpecificationException e) {
        LOGGER.warning(String.format("%s declares dependency '%s' -> '%s': '%s'",
            packageVersion.getId(),
            dependency.getVersionSpec(),
            dependency.getVersionRange(),
            e.getMessage()));
      }
    }
    return Predicates.alwaysTrue();
  }

  /**
   * Create a predicate for selecting candidates that were published BEFORE this package.
   * We can't know what version of the dependency this package was tested against, but we 
   * DO know that it could NOT have been tested on a package from the future.
   */
  private Predicate<PackageVersion> publishedBeforeConstraint() {

    if(packageVersion.getPublicationDate() == null) {
      return Predicates.alwaysTrue();
    } else {
      return new Predicate<PackageVersion>() {
        @Override
        public boolean apply(PackageVersion input) {
          // we are missing publication dates from some packages....
          // we will be strict here and refuse, if there are no matching criteria
          // we fallback above to the oldest version matching the version range criteria
          if(input.getPublicationDate() == null) {
            return false;
          }
          
          return packageVersion.getPublicationDate().after(input.getPublicationDate());
          
        }
      };
    }
    
  }


}