package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.DatastoreService;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.pipelines.ForEachEntityAsBean;


public class ReIndexPackageVersion extends ForEachEntityAsBean<PackageVersion> {

  private transient DatastoreService datastoreService;
  
  public ReIndexPackageVersion() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion entity) {
    
    if(entity.getPackageNameIndex() == null) {
      entity.setPackageName(entity.getPackageVersionId().getPackageName());

      ObjectifyService.ofy().transactionless().save().entity(entity);
    }
  }
}
