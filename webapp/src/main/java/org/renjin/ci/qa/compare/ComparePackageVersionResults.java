package org.renjin.ci.qa.compare;

import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import org.renjin.ci.model.*;
import org.renjin.ci.pipelines.EntityMapFunction;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * For a given PackageVersion, compare the results of two Renjin Versions
 */
public class ComparePackageVersionResults extends EntityMapFunction<PackageVersion> {

  private long reportId;
  private RenjinVersionId fromVersionId;
  private RenjinVersionId toVersionId;

  private transient DatastoreMutationPool pool;


  public ComparePackageVersionResults(long reportId, RenjinVersionId fromVersionId, RenjinVersionId toVersionId) {
    super(PackageVersion.class);
    this.reportId = reportId;
    this.fromVersionId = fromVersionId;
    this.toVersionId = toVersionId;
  }

  @Override
  public void beginSlice() {
    pool = DatastoreMutationPool.create();
  }

  @Override
  public void endSlice() {
    pool.flush();
  }

  @Override
  public void apply(PackageVersion packageVersion) {
    PackageStatus fromStatus = PackageDatabase.getStatus(packageVersion.getPackageVersionId(), fromVersionId);
    PackageStatus toStatus = PackageDatabase.getStatus(packageVersion.getPackageVersionId(), toVersionId);
    boolean needsSave = false;

    VersionComparisonEntry entry = new VersionComparisonEntry(reportId, packageVersion.getPackageVersionId());
    if(fromStatus.getBuildStatus() == BuildStatus.BUILT && toStatus.getBuildStatus() == BuildStatus.FAILED) {
      entry.setBuildDelta(Delta.REGRESSION);
      getContext().incrementCounter(VersionComparisonReport.Statistic.BUILD_REGRESSIONS.name());
      needsSave = true;

    } else if(fromStatus.getBuildStatus() == BuildStatus.FAILED && toStatus.getBuildStatus() == BuildStatus.BUILT) {
      entry.setBuildDelta(Delta.FIX);
      getContext().incrementCounter(VersionComparisonReport.Statistic.BUILD_FIXES.name());
      needsSave = true;
    }
    if(needsSave) {
      pool.put(ofy().save().toEntity(entry));
    }
  }
}
