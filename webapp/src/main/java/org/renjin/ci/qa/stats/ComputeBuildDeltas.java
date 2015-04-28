package org.renjin.ci.qa.stats;

import com.google.common.collect.Maps;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.util.*;
import java.util.logging.Logger;

/**
 * Identifies regressions/improvements in the package building process across successive
 * Renjin versions.
 * 
 * <p>This is a map reduce "mapper" function that updates the "buildDelta" flag for each
 * of the builds of a given PackageVersion. For example, if survey:2.5 fails to build against
 * Renjin 0.7.1514, but successfully builds against Renjin 0.7.1534, then this is an improvement
 * we want to highlight. 
 * 
 * <p>However, if we try to build again with Renjin 0.7.1560 and it fails, then that PackageBuild is
 * a regression to flag immediately.</p>
 */
public class ComputeBuildDeltas extends ForEachPackageVersion {

  private static final Logger LOGGER = Logger.getLogger(ComputeBuildDeltas.class.getName());

  @Override
  protected void apply(final PackageVersionId packageVersionId) {

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        List<PackageBuild> builds = PackageDatabase.getFinishedBuilds(packageVersionId);
        List<Object> toSave = new ArrayList<Object>();

        if (!builds.isEmpty()) {


          // Build a simplified list, mapping each renjin version in order to a build
          // If there have been multiple builds for a given Renjin Version, use the last build
          TreeMap<RenjinVersionId, PackageBuild> buildMap = Maps.newTreeMap();
          for (PackageBuild build : builds) {
            RenjinVersionId rv = build.getRenjinVersionId();
            PackageBuild lastBuild = buildMap.get(rv);

            if (lastBuild == null || build.getBuildNumber() > lastBuild.getBuildNumber()) {
              buildMap.put(rv, build);
            }
          }

          // Walk the Renjin versions and tag regressions/improvements


          PackageBuild lastBuild = null;

          for (PackageBuild build : buildMap.values()) {
            byte newDelta;
            if (lastBuild == null || lastBuild.isSucceeded() == build.isSucceeded()) {
              newDelta = 0; // no change            
            } else if (lastBuild.isSucceeded() && build.isFailed()) {
              newDelta = -1; // regression
            } else {
              newDelta = +1;
            }
            if (build.getBuildDelta() != newDelta) {
              build.setBuildDelta(newDelta);
              toSave.add(build);
            }
            
            LOGGER.info(String.format("Renjin %s: Build %d (%+d)", 
                build.getRenjinVersion(), 
                build.getBuildNumber(), 
                build.getBuildDelta()));

            lastBuild = build;
          }
          
          // Clear the deltas of any builds that have been subsuperceeded and ignored here
          for (PackageBuild build : builds) {
            if(!buildMap.containsValue(build)) {
              if(build.getBuildDelta() != 0) {
                build.setBuildDelta((byte)0);
                toSave.add(build);
              }
            }
          }

          ObjectifyService.ofy().save().entities(toSave);
        }
      }
    });
  }
}
