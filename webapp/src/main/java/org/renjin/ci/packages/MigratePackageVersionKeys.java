package org.renjin.ci.packages;

import com.google.appengine.api.datastore.*;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachEntity;

import java.util.logging.Logger;

public class MigratePackageVersionKeys extends ForEachEntity {
  
  private static final Logger LOGGER = Logger.getLogger(MigratePackageVersionKeys.class.getName());
  
  private transient DatastoreService datastore;
  
  @Override
  public String getEntityKind() {
    return "PackageVersion";
  }

  @Override
  public void beginSlice() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void map(Entity entity) {

    if(entity.getParent() == null) {

      PackageVersionId pvid = PackageVersionId.fromTriplet(entity.getKey().getName());

      Key packageKey = KeyFactory.createKey("Package", pvid.getPackageId().toString());
      Entity newEntity = new Entity("PackageVersion", pvid.getVersionString(), packageKey);
      newEntity.setPropertiesFrom(entity);

      LOGGER.info(entity.getKey() + " -> " + newEntity.getKey());

      datastore.put(newEntity);

      datastore.delete(entity.getKey());

    }
  }
}
