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
  public static final String OPEN_VERSION_RANGE = "[0,)";

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
    Map<String, Iterable<PackageVersion>> candidates = new HashMap<>();
    for (String packageName : declared.keySet()) {
      if (!CorePackages.isCorePackage(packageName)) {
        candidates.put(packageName, queryCandidates(packageName));
      }
    }

    // Now find candidates that meet both declared criteria and availability of builds
    ResolvedDependencySet result = new ResolvedDependencySet();
    result.setComplete(true);

    for (String packageName : declared.keySet()) {
      ResolvedDependency selected = select(declared.get(packageName), candidates.get(packageName));
      
      if(selected == null) {
        result.setComplete(false);
        result.getMissingPackages().add(packageName);
      } else {
        result.getDependencies().add(selected);
      }
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
          !"R".equals(dependency.getName())) {

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

  private QueryResultIterable<PackageVersion> queryCandidates (String packageName) {
    // Assume CRAN for now: need to consider bioConductor/third party later
    PackageId packageId = new PackageId("org.renjin.cran", packageName);

    // Initiate a query for all the available SOURCE versions: this will
    // run asynchronously until we request the first result.
    return ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .ancestor(Package.key(packageId)).iterable();
  }


  private ResolvedDependency select(PackageDependency declared, Iterable<PackageVersion> candidates) {

    List<PackageVersion> candidateList = Lists.newArrayList(candidates);

    // Sort in descending order, so we can look from most recent to the oldest
    Collections.sort(candidateList, PackageVersion.orderByVersion().reverse());

    Predicate<PackageVersion> versionRange = versionConstraint(declared);

    LOGGER.info(String.format("Resolving dependency %s satisfying %s from among: %s",
        declared, versionRange, candidateList));


    // First find the most recent meeting the declared criteria with a successful build...
    for (PackageVersion version : candidateList) {
      if(version.hasSuccessfulBuild() && versionRange.apply(version)) {
        return new ResolvedDependency(version.getLastSuccessfulBuildId());
      }
    }

    // Failing that, return the latest as unresolved dependency
    for (PackageVersion version : candidateList) {
      if(versionRange.apply(version)) {
        return new ResolvedDependency(version.getPackageVersionId());
      }
    }

    // No beans...
    return null;
  }


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

}