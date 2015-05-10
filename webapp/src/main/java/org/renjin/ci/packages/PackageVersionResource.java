
package org.renjin.ci.packages;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

  @GET
  @Produces("text/html")
  public Viewable getPage() {
    VersionViewModel viewModel = new VersionViewModel(packageVersion);
    viewModel.setBuilds(PackageDatabase.getBuilds(packageVersionId).list());
    Map<String, Object> model = new HashMap<>();
    model.put("version", viewModel);

    return new Viewable("/packageVersion.ftl", model);
  }

  @Path("build/{buildNumber}")
  public PackageBuildResource getBuild(@PathParam("buildNumber") int buildNumber) {
    return new PackageBuildResource(packageVersion.getPackageVersionId(), buildNumber);
  }

  /**
   * Allocate a new build number for this package version
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

        ObjectifyService.ofy().save().entities(packageBuild, packageVersion);

        return packageBuild;
      }
    });
  }
  

  @GET
  @Path("check")
  public Response check() {
    ReComputeBuildDeltas markBuildDeltas = new ReComputeBuildDeltas();
    markBuildDeltas.map(PackageVersion.key(packageVersionId).getRaw());

    return Response.ok("Done").build();
  }
  
  @Path("resolveDependencies")
  public DependencyResolution resolveDependencies() {
    return new DependencyResolution(packageVersion);
  }
}
