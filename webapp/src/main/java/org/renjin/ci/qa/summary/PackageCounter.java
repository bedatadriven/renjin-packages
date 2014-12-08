package org.renjin.ci.qa.summary;

import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;
import com.google.common.collect.Sets;
import org.renjin.ci.model.RenjinVersionId;


public class PackageCounter extends Reducer<RenjinVersionId, String, Long> {

  @Override
  public void reduce(RenjinVersionId key, ReducerInput<String> values) {
    long distinctPackages = Sets.newHashSet(values).size();
    emit(distinctPackages);
  }
}
