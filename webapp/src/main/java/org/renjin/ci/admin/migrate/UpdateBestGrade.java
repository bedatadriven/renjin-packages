package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.HashMap;

public class UpdateBestGrade extends ForEachEntityAsBean<PackageBuild> {

  private transient HashMap<PackageId, Package> packageCache;

  public UpdateBestGrade() {
    super(PackageBuild.class);
  }

  @Override
  public void beginSlice() {
    super.beginSlice();
    packageCache = new HashMap<>();
  }


  @Override
  public void apply(final PackageBuild build) {

    if(build.getGradeInteger() == PackageBuild.GRADE_F) {
      return;
    }

    // See if we can exit early...
    Package cachedPackage = packageCache.get(build.getPackageId());
    if(cachedPackage != null && !isBetter(cachedPackage, build)) {
      return;
    }

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        Package packageEntity = PackageDatabase.getPackage(build.getPackageId()).now();
        if(packageEntity == null) {
          return;
        }

        if(isBetter(packageEntity, build)) {
          packageEntity.setBestGrade(build.getGrade());
          packageEntity.setBestPackageVersion(build.getVersion());
          PackageDatabase.ofy().save().entity(packageEntity).now();
        }
        packageCache.put(build.getPackageId(), packageEntity);
      }
    });
  }

  private boolean isBetter(Package packageEntity, PackageBuild build) {
    if(build.getGradeInteger() == 0) {
      return false;
    }
    if(packageEntity.getBestGradeInteger() > build.getGradeInteger()) {
      return false;
    }
    if(build.getGradeInteger() > packageEntity.getBestGradeInteger()) {
      return true;
    }
    // Otherwise, need to compare package versions.
    if(packageEntity.getBestPackageVersion() == null) {
      return true;
    }
    if(build.getPackageVersionId().isNewer(packageEntity.getBestPackageVersionId())) {
      return true;
    }
    return false;
  }


  @Override
  public void endShard() {
    super.endShard();
  }
}
