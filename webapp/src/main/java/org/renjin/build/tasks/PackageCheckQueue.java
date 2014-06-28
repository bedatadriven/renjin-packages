package org.renjin.build.tasks;

import com.google.common.collect.Maps;
import com.googlecode.objectify.VoidWork;
import org.renjin.build.model.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 */
public class PackageCheckQueue {

  private static final Logger LOGGER = Logger.getLogger(PackageCheckQueue.class.getName());

  /**
   * (Possibly) updates the status of a PackageVersion-Renjin check
   */
  public void updateStatus(final PackageVersion packageVersion, final RenjinVersionId renjinVersion) {

    LOGGER.log(Level.INFO, "Updating check status for " + packageVersion.getId() + " @ Renjin " + renjinVersion);

    // Determine which dependencies are built
    final Map<PackageVersionId, PackageBuildId> built = resolveBuilds(packageVersion.getDependencyIdSet());

    final Set<PackageVersionId> notBuilt = packageVersion.getDependencyIdSet();
    notBuilt.removeAll(built.keySet());

    // Now update
    ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

//        PackageStatus status = PackageDatabase.getStatus(
//            packageVersion.getPackageVersionId(),
//            renjinVersion);

        PackageStatus status = new PackageStatus(packageVersion.getPackageVersionId(), renjinVersion);
        if(status.getBuildStatus() == null) {
          status.setBuildStatus(BuildStatus.ORPHANED);
        }

        if(!packageVersion.isCompileDependenciesResolved()) {
          LOGGER.info("Compile-time dependencies are not yet resolved, setting status => ORPHANED");
          status.setBuildStatus(BuildStatus.ORPHANED);

        } else {

          // Don't mess with those already built
          if(status.getBuildStatus() != BuildStatus.BUILT) {
            status.setDependenciesFrom(built.values());
            status.setBlockingDependenciesFrom(notBuilt);

            if( status.getBuildStatus() == BuildStatus.ORPHANED) {
              LOGGER.info("All dependencies resolved, transitioning to BLOCKED state");
              status.setBuildStatus(BuildStatus.BLOCKED);
            }

            // can we transition to READY?
            if( status.getBuildStatus() == BuildStatus.BLOCKED) {

              if(status.getBlockingDependencies().isEmpty()) {

                LOGGER.info("All dependencies built, transitioning to READY state");
                status.setBuildStatus(BuildStatus.READY);

              } else {
                LOGGER.info("Status remains blocked: waiting on " + status.getBlockingDependencies());
              }
            }
          }
        }
        PackageDatabase.save(status);
      }
    });
  }

  /**
   * Resolves this packages dependencies to a set of builds
   * @param dependencies
   * @return
   */
  public Map<PackageVersionId, PackageBuildId> resolveBuilds(Set<PackageVersionId> dependencies) {
    Map<PackageVersionId, PackageBuildId> builds = Maps.newHashMap();
    Collection<PackageStatus> depStatus = PackageDatabase.getStatus(dependencies, RenjinVersionId.RELEASE);

    for(PackageStatus status : depStatus) {
      if(status.getBuildStatus() == BuildStatus.BUILT) {
        builds.put(status.getPackageVersionId(), status.getBuild());
      }
    }

    return builds;
  }
}