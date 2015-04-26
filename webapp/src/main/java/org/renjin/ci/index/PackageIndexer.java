package org.renjin.ci.index;

import com.googlecode.objectify.VoidWork;
import org.renjin.ci.model.*;
import org.renjin.ci.model.Package;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.Collections;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.Collections.max;

public class PackageIndexer extends ForEachEntityAsBean<Package> {

    public PackageIndexer() {
        super(Package.class);
    }

    @Override
    public void apply(final Package entity) {
        
        // Query the versions and builds outside of the transaction
        final List<PackageVersion> packageVersions = PackageDatabase.queryPackageVersions(entity);
        final List<PackageBuild> packageBuilds = PackageDatabase.queryPackageBuilds(entity.getGroupId(), entity.getName());

        final PackageVersion latestVersion = Collections.max(packageVersions, PackageVersion.orderByVersion());
        final PackageDescription description = latestVersion.parseDescription();

        // Update the package statistics 
        ofy().transact(new VoidWork() {
            @Override
            public void vrun() {

                Package toUpdate = ofy().load().entity(entity).now();
                toUpdate.setTitle(description.getTitle());
                toUpdate.setLatestVersion(findLatestVersion(packageVersions));
                toUpdate.setLastGoodBuild(findLastGoodBuild(packageBuilds));
                toUpdate.setRenjinRegression(checkRegressionForRegression(packageBuilds));
          
                ofy().save().entity(toUpdate);
            }
        });
        
        PackageSearchIndex.updateIndex(latestVersion);
    }


    private String findLastGoodBuild(List<PackageBuild> packageBuilds) {
        PackageBuild lastGood = null;
        Collections.sort(packageBuilds, PackageBuild.orderByNumber());
        for(PackageBuild build : packageBuilds) {
            if(build.getOutcome() == BuildOutcome.SUCCESS) {
                lastGood = build;
            }
        }
        if(lastGood == null) {
            return null;
        } else {
            return lastGood.getBuildVersion();
        }
    }

    private String findLatestVersion(List<PackageVersion> packageVersions) {
        return max(packageVersions, PackageVersion.orderByVersion()).getVersion().toString();
    }


    private boolean checkRegressionForRegression(List<PackageBuild> packageBuilds) {
        boolean hadSuccess = false;
        boolean regressed = false;
        
        Collections.sort(packageBuilds, PackageBuild.orderByNumber());
        for(PackageBuild build : packageBuilds) {
            if(build.getOutcome() != null) {
                switch (build.getOutcome()) {
                    case SUCCESS:
                        hadSuccess = true;
                        regressed = false;
                        break;
                    case ERROR:
                    case FAILURE:
                        if (hadSuccess) {
                            regressed = true;
                        }
                }
            }
        }
        return regressed;
    }
}
