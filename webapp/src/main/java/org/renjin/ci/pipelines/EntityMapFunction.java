package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;
import org.renjin.ci.model.PackageDatabase;

import static com.googlecode.objectify.ObjectifyService.ofy;

public abstract class EntityMapFunction<T> extends MapOnlyMapper<Entity, Void> {

  static {
    PackageDatabase.init();
  }

  private final Class<T> clazz;

  public EntityMapFunction(Class<T> entityClass) {
    this.clazz = entityClass;
  }

  public Class<T> getEntityClass() {
    return this.clazz;
  }

  public String getEntityName() {
    return this.clazz.getSimpleName();
  }

  @Override
  public final void map(Entity value) {
    apply(ofy().load().<T>fromEntity(value));
  }

  public abstract void apply(T entity);


  public String getJobName() {
    return getClass().getSimpleName();
  }
}
