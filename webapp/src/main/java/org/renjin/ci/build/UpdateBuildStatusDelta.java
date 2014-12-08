package org.renjin.ci.build;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.*;
import org.renjin.ci.pipelines.EntityMapFunction;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Update PackageBuildStatus with delta flags
 */
public class UpdateBuildStatusDelta extends EntityMapFunction<PackageVersion> {

  private static final Logger LOGGER = Logger.getLogger(UpdateBuildStatusDelta.class.getName());

  public UpdateBuildStatusDelta() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion packageVersion) {

    // Query all the status of this PV for all versions of Renjin
    List<PackageStatus> status = PackageDatabase.getAllStatusForPackageVersion(packageVersion.getPackageVersionId());

    // Order by version
    Collections.sort(status, PackageStatus.orderByRenjinVersion());

    // and filter on only those builds that completed
    Iterator<PackageStatus> it = Iterators.filter(status.iterator(), new Completed());

    if(!it.hasNext()) {
      LOGGER.info("Package Version " + packageVersion + " has no completed builds.");
      return;
    }


    PackageStatus first = it.next();
    boolean wasBuilding = first.getBuildStatus() == BuildStatus.BUILT;

    List<PackageStatus> toUpdate = Lists.newArrayList();

    while (it.hasNext()) {
      PackageStatus next = it.next();
      boolean buildingNow = next.getBuildStatus() == BuildStatus.BUILT;

      if (wasBuilding && !buildingNow) {
        next.setBuildDelta(Delta.REGRESSION);

      } else if (!wasBuilding && buildingNow) {
        next.setBuildDelta(Delta.FIX);

      } else {
        next.setBuildDelta(Delta.NO_CHANGE);
      }

      LOGGER.info(next.getRenjinVersionId() + " " + next.getBuildStatus() + ": " + next.getBuildDelta());

      toUpdate.add(next);
      wasBuilding = buildingNow;
    }

    ObjectifyService.ofy().save().entities(toUpdate);
  }

  private static class Completed implements Predicate<PackageStatus> {
    @Override
    public boolean apply(PackageStatus input) {
      return input.getBuildStatus() == BuildStatus.BUILT ||
          input.getBuildStatus() == BuildStatus.FAILED;
    }
  }
}