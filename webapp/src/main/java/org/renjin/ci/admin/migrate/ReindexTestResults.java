package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import org.renjin.ci.pipelines.ForEachEntity;


public class ReindexTestResults extends ForEachEntity {

  private transient DatastoreService datastore;

  @Override
  public String getEntityKind() {
    return "PackageTestResult";
  }

  @Override
  public void beginSlice() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void map(Entity value) {
    value.setProperty("name", value.getKey().getName());
    
    datastore.put(value);
  }
}
