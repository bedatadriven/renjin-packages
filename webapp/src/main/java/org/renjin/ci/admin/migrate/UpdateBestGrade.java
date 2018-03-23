package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.HashMap;

public class UpdateBestGrade extends ForEachEntityAsBean<PackageBuild> {

  public static final RenjinVersionId MIN_RENJIN_VERSION = RenjinVersionId.valueOf("0.8.0");
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

    if(!build.getRenjinVersionId().isNewerThan(MIN_RENJIN_VERSION)) {
      return;
    }

    if(build.getOutcome() != BuildOutcome.SUCCESS) {
      return;
    }

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
          packageEntity.setGrade(build.getGrade());
          packageEntity.setBestVersion(build.getVersion());
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
    if(packageEntity.getGradeInteger() > build.getGradeInteger()) {
      return false;
    }
    if(build.getGradeInteger() > packageEntity.getGradeInteger()) {
      return true;
    }
    // Otherwise, need to compare package versions.
    if(packageEntity.getBestVersion() == null) {
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
