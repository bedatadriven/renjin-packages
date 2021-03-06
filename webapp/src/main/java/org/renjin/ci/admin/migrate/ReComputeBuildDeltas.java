package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.packages.DeltaBuilder;
import org.renjin.ci.pipelines.ForEachPackageVersion;

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
public class ReComputeBuildDeltas extends ForEachPackageVersion {

  private static final Logger LOGGER = Logger.getLogger(ReComputeBuildDeltas.class.getName());

  @Override
  protected void apply(final PackageVersionId packageVersionId) {

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        DeltaBuilder.update(packageVersionId);
      }
    });
  }
}
