package org.renjin.ci.qa.stats;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.RenjinVersionStat;

/**
 * Reduces a set of [RenjinVersionId:DeltaType, +/-] to a {@code RenjinVersionDeltas}
 */
public class DeltaReducer extends Reducer<String, Integer, Entity> {
  @Override
  public void reduce(String key, ReducerInput<Integer> values) {

    int regressions = 0;
    int progressions = 0;
    
    while(values.hasNext()) {
      int delta = values.next();
      if(delta < 0) {
        regressions++;
      } else {
        progressions++;
      }
    }

    RenjinVersionStat deltas = new RenjinVersionStat();
    deltas.setId(key);
    deltas.setRegressionCount(regressions);
    deltas.setProgressionCount(progressions);

    emit(ObjectifyService.ofy().save().toEntity(deltas));
  }
}
