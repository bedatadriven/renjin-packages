package org.renjin.ci.stats;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.RenjinVersionStat;

import java.util.HashSet;
import java.util.Set;

/**
 * Reduces a set of [RenjinVersionId, DeltaType] to a {@code RenjinVersionStat}
 */
public class DeltaReducer extends Reducer<DeltaKey, DeltaValue, Entity> {
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
  
    RenjinVersionStat deltas = new RenjinVersionStat();
    deltas.setId(key.toString());
    deltas.setRegressionCount(regressions.size());
    deltas.setProgressionCount(progressions.size());

    emit(ObjectifyService.ofy().save().toEntity(deltas));
  }
}
