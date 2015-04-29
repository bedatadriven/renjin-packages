package org.renjin.ci.pipelines;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersionId;


public class SplitPackageVersion extends ForEachPackageVersion {
  @Override
  protected void apply(final PackageVersionId packageVersionId) {
    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();
        PackageDescription description = packageVersion.loadDescription();
        
        packageVersion.setTitle(description.getTitle());
        try {
          packageVersion.setPublicationDate(description.getPublicationDate().toDate());

        } catch (Exception ignored) {
        }
        
        ObjectifyService.ofy().save().entities(packageVersion);
      }
    });
  }
}
