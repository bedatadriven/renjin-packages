
package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.renjin.ci.build.BuildResource;
import org.renjin.ci.model.PackageBuild;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;

/**
 * Specific version of a package
 */
public class PackageVersionResource {
  private final PackageVersion packageVersion;
  private PackageVersionId packageVersionId;

  public PackageVersionResource(PackageVersion packageVersion) {
    this.packageVersionId = packageVersion.getPackageVersionId();
    this.packageVersion = packageVersion;
  }

  @Path("build/{buildNumber}")
  public BuildResource getBuild(@PathParam("buildNumber") int buildNumber) {
    return new BuildResource(packageVersion.getPackageVersionId(), buildNumber);
  }

  /**
   * Allocate a new build number for this package version
   * @return
   */
  @POST
  @Path("builds")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public PackageBuild startBuild(@FormParam("renjinVersion") final String renjinVersion) {
    return ObjectifyService.ofy().transactNew(new Work<PackageBuild>() {
      @Override
      public PackageBuild run() {
        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

        // increment next build number
        long nextBuild = packageVersion.getLastBuildNumber() + 1;
        packageVersion.setLastBuildNumber(nextBuild);

        PackageBuild packageBuild = new PackageBuild(packageVersionId, nextBuild);
        packageBuild.setRenjinVersion(renjinVersion);
        packageBuild.setStartTime(new Date().getTime());
        packageBuild.setDependencies(packageVersion.getDependencies());

        ObjectifyService.ofy().save().entities(packageBuild, packageVersion);

        return packageBuild;
      }
    });
  }


  @GET
  @Path("dependencies")
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getVersionMetadata() {

    return Lists.newArrayList(packageVersion.getDependencies());
  }
}
