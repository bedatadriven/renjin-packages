package org.renjin.ci.packages;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachEntity;

import java.util.logging.Level;
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
      
      PackageBuildId id;

      try {

        String[] parts = entity.getKey().getName().split(":");
        PackageId packageId = new PackageId(parts[0], parts[1]);
        String version;
        String buildNumber;

        if(parts.length == 4) {
          version = parts[2];
          buildNumber = parts[3];
        } else {
          int buildNumberStart = parts[2].indexOf("-b");
          version = parts[2].substring(0, buildNumberStart);
          buildNumber = parts[2].substring(buildNumberStart+2);
        }

        id = new PackageBuildId(new PackageVersionId(packageId, version), Long.parseLong(buildNumber));

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error parsing build id : " + entity.getKey(), e);
        return;
      }
      
      Key packageVersionKey = PackageVersion.key(id.getPackageVersionId()).getRaw();
      Entity newEntity = new Entity("PackageBuild", id.getBuildNumber(), packageVersionKey);
      newEntity.setPropertiesFrom(entity);

      LOGGER.info(entity.getKey() + " -> " + newEntity.getKey());

      datastore.put(newEntity);
      
    }
  }
}
