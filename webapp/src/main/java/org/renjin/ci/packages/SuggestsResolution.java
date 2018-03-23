package org.renjin.ci.packages;

import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.ResolvedDependencySet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves suggests dependencies
 */
public class SuggestsResolution {

  public ResolvedDependencySet resolve(List<String> specs) {

    List<ResolvedDependency> resolved = new ArrayList<>();

    Map<PackageId, LoadResult<Package>> packages = new HashMap<>();
    List<LoadResult<PackageVersion>> versions = new ArrayList<>();

    List<PackageDependency> dependencies = new ArrayList<>();
    for (String spec : specs) {
      dependencies.add(new PackageDependency(spec));
    }

    for (PackageDependency dependency : dependencies) {

      String name = dependency.getName();

      PackageId cranId = new PackageId(PackageId.CRAN_GROUP, name);
      PackageId biocId = new PackageId(PackageId.BIOC_GROUP, name);

      packages.put(cranId, PackageDatabase.getPackage(cranId));
      packages.put(biocId, PackageDatabase.getPackage(biocId));
    }

    for (PackageDependency dependency : dependencies) {

      String name = dependency.getName();

      PackageId cranId = new PackageId(PackageId.CRAN_GROUP, name);
      PackageId biocId = new PackageId(PackageId.BIOC_GROUP, name);

      LoadResult<Package> thePackage;
      if (packages.get(cranId).now() != null) {
        thePackage = packages.get(cranId);
      } else {
        thePackage = packages.get(biocId);
      }
      if (thePackage != null && thePackage.now() != null) {
        if (thePackage.now().isReplaced()) {
          ResolvedDependency resolvedDependency = new ResolvedDependency(thePackage.now().getLatestVersionId());
          resolvedDependency.setReplacementVersion(thePackage.now().getLatestReplacementVersion());
          resolved.add(resolvedDependency);
        } else {
          versions.add(PackageDatabase.ofy().load().key(PackageVersion.key(thePackage.now().getLatestVersionId())));
        }
      }
    }

    for (LoadResult<PackageVersion> version : versions) {
      PackageVersion latestVersion = version.now();
      if(latestVersion != null) {
        ResolvedDependency resolvedDependency = new ResolvedDependency(latestVersion.getPackageVersionId());
        if(latestVersion.getLastSuccessfulBuildNumber() != 0) {
          resolvedDependency.setBuildNumber(latestVersion.getLastSuccessfulBuildNumber());
        }
        resolved.add(resolvedDependency);
      }
    }

    return new ResolvedDependencySet(resolved);
  }

}
