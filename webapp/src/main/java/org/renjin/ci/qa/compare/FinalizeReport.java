package org.renjin.ci.qa.compare;

import com.google.appengine.tools.mapreduce.MapReduceResult;
import com.google.appengine.tools.pipeline.Job2;
import com.google.appengine.tools.pipeline.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.renjin.ci.model.VersionComparisonReport;

import java.util.HashMap;
import java.util.Map;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.renjin.ci.model.VersionComparisonReport.Statistic;

public class FinalizeReport extends Job2<Void, Key<VersionComparisonReport>, MapReduceResult<Void>> {

  @Override
  public Value<Void> run(final Key<VersionComparisonReport> reportKey, final MapReduceResult<Void> result) throws Exception {
    return immediate(ofy().transactNew(new Work<Void>() {
      @Override
      public Void run() {

        Map<String, Long> statistics = new HashMap<>();
        for(Statistic statistic : Statistic.values()) {
          statistics.put(statistic.name(), result.getCounters().getCounter(statistic.name()).getValue());
        }

        VersionComparisonReport report = ofy().load().key(reportKey).safe();
        report.setComplete(true);
        report.setStatistics(statistics);

        ObjectifyService.ofy().save().entity(report);
        return null;
      }
    }));
  }
}
