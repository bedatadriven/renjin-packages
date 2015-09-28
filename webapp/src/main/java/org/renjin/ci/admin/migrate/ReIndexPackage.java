package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

/**
 * Created by alex on 28-9-15.
 */
public class ReIndexPackage extends ForEachEntityAsBean<Package> {

  public ReIndexPackage() {
    super(Package.class);
  }

  @Override
  public void apply(Package entity) {
    entity.setName(entity.getPackageId().getPackageName());

    ObjectifyService.ofy().transactionless().save().entity(entity);
  }
}
