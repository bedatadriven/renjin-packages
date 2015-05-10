package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.archive.BuildLogs;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;


public class UpdateNeedsCompilation extends ForEachPackageVersion {

  private static final String NATIVE_COMPILATION_SUCCESS = "Soot finished on ";
  private static final String NATIVE_COMPILATION_FAILURE = "Compilation of GNU R sources failed";
  
  private static final Logger LOGGER = Logger.getLogger(UpdateNeedsCompilation.class.getName());

  @Override
  protected void apply(final PackageVersionId packageVersionId) {

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        
        List<Object> toSave = new ArrayList<Object>();

        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();
        packageVersion.setNeedsCompilation(packageVersion.loadDescription().isNeedsCompilation());
        toSave.add(packageVersion);

        if(packageVersion.isNeedsCompilation()) {

          for (PackageBuild packageBuild : PackageDatabase.getBuilds(packageVersionId)) {
            if (packageBuild.isSucceeded() && isMissing(packageBuild.getNativeOutcome())) {
              try {
                packageBuild.setNativeOutcome(parseOutcome(packageBuild));
                
                LOGGER.info(format("%s: parsed outcome %s", packageVersionId, packageBuild.getNativeOutcome()));
              } catch (Exception e) {
                LOGGER.info(format("%s: failed to parse compilation output: %s", packageVersionId, e.getMessage()));
              }
            }
          }
        }
        
        ObjectifyService.ofy().save().entities(toSave);
      }
    });

  }

  private NativeOutcome parseOutcome(PackageBuild packageBuild) {
    
    String logFile = BuildLogs.tryFetchLog(packageBuild.getId());
    if(logFile == null) {
      return null;
    }
    
    if(logFile.contains(NATIVE_COMPILATION_FAILURE)) {
      return NativeOutcome.FAILURE;
    } else if(logFile.contains(NATIVE_COMPILATION_SUCCESS)) {
      return NativeOutcome.SUCCESS;
    }
    return null;
  }

  private boolean isMissing(NativeOutcome nativeOutcome) {
    return nativeOutcome == null || nativeOutcome == NativeOutcome.NA;
  }
}
