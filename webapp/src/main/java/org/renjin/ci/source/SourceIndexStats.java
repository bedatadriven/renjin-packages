package org.renjin.ci.source;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

/**
 * Statistics on the PackageSource entities.
 */
public class SourceIndexStats {
  
  private long count;
  private long bytes;
  
  public static SourceIndexStats get() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query("__Stat_Kind__")
        .setFilter(new Query.FilterPredicate("kind_name", Query.FilterOperator.EQUAL, "PackageSource"));

    Entity entity = datastore.prepare(query).asSingleEntity();
    SourceIndexStats stats = new SourceIndexStats();
    stats.count = (Long) entity.getProperty("count");
    stats.bytes = (Long) entity.getProperty("bytes");
    
    return stats;
  }

  public long getCount() {
    return count;
  }

  public long getBytes() {
    return bytes;
  }
  
  public double getGigabytes() {
    double b = getBytes();
    double kilobytes = b / 1024d;
    double megabytes = kilobytes / 1024d;
    double gigabytes = megabytes / 1024d;
    return gigabytes;
  }
}
