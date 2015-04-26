package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;


public abstract class ForEachEntity extends MapOnlyMapper<Entity, Void> {
  
  public abstract String getEntityKind();

  public String getJobName() {
    return getClass().getSimpleName();
  }
}
