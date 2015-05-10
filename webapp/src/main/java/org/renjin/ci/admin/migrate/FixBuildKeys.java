package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachEntity;

import java.util.logging.Logger;


public class FixBuildKeys extends ForEachEntity {

  private static final Logger LOGGER = Logger.getLogger(FixBuildKeys.class.getName());
  
  private transient DatastoreService datastoreService;

  @Override
  public String getEntityKind() {
    return "PackageBuild";
  }

  @Override
  public void map(Entity oldEntity) {
    
    
    if(oldEntity.getKey().getName() != null) {


      if(datastoreService == null) {
        datastoreService = DatastoreServiceFactory.getDatastoreService();
      }
      
      // org.renjin.cran:accrued:1.0-b152
      String keyParts[] = oldEntity.getKey().getName().split(":");
      String groupId = keyParts[0];
      String packageName = keyParts[1];
      String versionParts[] = keyParts[2].split("-b");

      if (versionParts.length != 2) {
        throw new IllegalStateException("Couldn't parse version string: " + keyParts[2]);
      }

      PackageVersionId pvid = new PackageVersionId(groupId, packageName, versionParts[0]);

      long buildNumber;
      try {
        buildNumber = Long.parseLong(versionParts[1]);
      } catch (Exception e) {
        backupAndDeleteEntity(oldEntity);
        return;
      }

      String renjinVersion = (String)oldEntity.getProperty("renjinVersion");
      if(renjinVersion == null || renjinVersion.contains("SNAPSHOT")) {
        backupAndDeleteEntity(oldEntity);
        return;
      }

      Key newKey = PackageBuild.key(pvid, buildNumber).getRaw();
      Entity entity = new Entity(newKey);
      entity.setPropertiesFrom(oldEntity);

      PackageBuild build = ObjectifyService.ofy().load().fromEntity(entity);

      ObjectifyService.ofy().transactionless().save().entity(build).now();
      datastoreService.delete(oldEntity.getKey());

      LOGGER.warning(oldEntity.getKey().getName() + " => " + newKey);

    }
  }

  private void backupAndDeleteEntity(Entity oldEntity) {
    Entity backupEntity = new Entity("PackageBuildOld", oldEntity.getKey().getName());
    backupEntity.setPropertiesFrom(oldEntity);
    
    LOGGER.severe("Backing up and deleting " + oldEntity.getKey() + " => " + backupEntity.getKey());
    
    datastoreService.put(backupEntity);
    datastoreService.delete(oldEntity.getKey());
    
  }
}
