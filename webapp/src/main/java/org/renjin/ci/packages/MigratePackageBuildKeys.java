package org.renjin.ci.packages;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.pipelines.ForEachEntity;

import java.util.logging.Logger;

public class MigratePackageBuildKeys extends ForEachEntity {
  
  private static final Logger LOGGER = Logger.getLogger(MigratePackageBuildKeys.class.getName());

  private transient DatastoreService datastore;


  @Override
  public String getEntityKind() {
    return "PackageBuild";
  }

  @Override
  public void beginSlice() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void map(Entity entity) {

    if(entity.getParent() == null) {

      PackageBuildId id = new PackageBuildId(entity.getKey().getName());

      Key packageVersionKey = PackageVersion.key(id.getPackageVersionId()).getRaw();
      Entity newEntity = new Entity("PackageBuild", id.getBuildNumber(), packageVersionKey);
      newEntity.setPropertiesFrom(entity);

      LOGGER.info(entity.getKey() + " -> " + newEntity.getKey());

      datastore.put(newEntity);

      datastore.delete(entity.getKey());

    }
  }
}
