package org.renjin.ci.stats;

import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;
import org.renjin.ci.datastore.RenjinVersionStat;

import java.util.HashSet;
import java.util.Set;

/**
 * Reduces a set of [RenjinVersionId, DeltaType] to a {@code RenjinVersionStat}
 */
public class DeltaReducer extends Reducer<DeltaKey, DeltaValue, RenjinVersionStat> {
  @Override
  public void reduce(DeltaKey key, ReducerInput<DeltaValue> values) {

    Set<String> regressions = new HashSet<>();
    Set<String> progressions = new HashSet<>();
    
    while(values.hasNext()) {
      DeltaValue delta = values.next();
      if(delta.getDelta() < 0) {
        regressions.add(delta.getKey());
      } else {
        progressions.add(delta.getKey());
      }
    }
  
    RenjinVersionStat stat = new RenjinVersionStat();
    stat.setName(key.getType().name());
    stat.setRenjinVersion(key.getRenjinVersionId());
    stat.setRegressionCount(regressions.size());
    stat.setProgressionCount(progressions.size());

    emit(stat);
  }
}
