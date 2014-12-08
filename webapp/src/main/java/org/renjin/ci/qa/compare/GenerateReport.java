package org.renjin.ci.qa.compare;

import com.google.appengine.tools.mapreduce.MapReduceResult;
import com.google.appengine.tools.pipeline.FutureValue;
import com.google.appengine.tools.pipeline.Job0;
import com.google.appengine.tools.pipeline.Value;
import com.googlecode.objectify.Key;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.model.VersionComparisonReport;
import org.renjin.ci.pipelines.Pipelines;

public class GenerateReport extends Job0<Void> {

  private long reportId;
  private RenjinVersionId fromVersion;
  private RenjinVersionId toVersion;

  public GenerateReport(long reportId, RenjinVersionId fromVersion, RenjinVersionId toVersion) {
    this.reportId = reportId;
    this.fromVersion = fromVersion;
    this.toVersion = toVersion;
  }

  @Override
  public Value<Void> run() throws Exception {

    Value<Key<VersionComparisonReport>> reportKey = immediate(Key.create(VersionComparisonReport.class, reportId));
    FutureValue<MapReduceResult<Void>> mapResult = futureCall(
        Pipelines.newMapJob(new ComparePackageVersionResults(reportId, fromVersion, toVersion)));

    return futureCall(new FinalizeReport(), reportKey, mapResult);
  }
}
