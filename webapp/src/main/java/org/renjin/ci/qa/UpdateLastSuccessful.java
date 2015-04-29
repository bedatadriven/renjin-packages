package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.util.logging.Level;
import java.util.logging.Logger;


public class UpdateLastSuccessful extends ForEachPackageVersion {


  private static final Logger LOGGER = Logger.getLogger(UpdateLastSuccessful.class.getName());

  @Override
  protected void apply(final PackageVersionId packageVersionId) {

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        long lastSuccessful = 0;

        try {

          QueryResultIterable<PackageBuild> builds = PackageDatabase.getBuilds(packageVersionId).iterable();
          
          for (PackageBuild build : builds) {
            if (build.getOutcome() == BuildOutcome.SUCCESS) {
              lastSuccessful = build.getBuildNumber();
            }
          }
        } catch (Exception e) {
          // malformed keys, ignore
          LOGGER.log(Level.SEVERE, "Exception iterating over build for " + packageVersionId, e);
          return;
        }
        
        
        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();
        if(lastSuccessful != packageVersion.getLastSuccessfulBuildNumber()) {
          packageVersion.setLastSuccessfulBuildNumber(lastSuccessful);
        }

      }
    });

  }
}
