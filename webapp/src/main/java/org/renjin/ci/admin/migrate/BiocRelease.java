package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import static com.googlecode.objectify.ObjectifyService.ofy;


public class BiocRelease extends ForEachPackageVersion {

  @Override
  protected void apply(final PackageVersionId packageVersionId) {
    if(packageVersionId.getGroupId().equals("org.renjin.bioconductor")) {
      ofy().transact(new VoidWork() {
        @Override
        public void vrun() {
          PackageVersion pv = ofy().load().key(PackageVersion.key(packageVersionId)).safe();
          pv.setBioconductorRelease("3.2");
          ofy().save().entity(pv).now();
        }
      });
    }
  }
}
