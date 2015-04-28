package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Entity;
import org.renjin.ci.datastore.PackageDatabase;

import static com.googlecode.objectify.ObjectifyService.ofy;

public abstract class ForEachEntityAsBean<T> extends ForEachEntity {

  static {
    PackageDatabase.init();
  }

  private final Class<T> clazz;

  public ForEachEntityAsBean(Class<T> entityClass) {
    this.clazz = entityClass;
  }

  public Class<T> getEntityClass() {
    return this.clazz;
  }

  @Override
  public String getEntityKind() {
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
