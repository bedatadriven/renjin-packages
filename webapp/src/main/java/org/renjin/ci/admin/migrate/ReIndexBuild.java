package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.DatastoreService;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.pipelines.ForEachEntityAsBean;


public class ReIndexBuild extends ForEachEntityAsBean<PackageBuild> {

  private transient DatastoreService datastoreService;
  
  public ReIndexBuild() {
    super(PackageBuild.class);
  }

  @Override
  public void apply(PackageBuild entity) {
    
    ObjectifyService.ofy().transactionless().save().entity(entity);
      
  }
}
