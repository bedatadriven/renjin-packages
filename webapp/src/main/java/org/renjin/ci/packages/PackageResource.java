package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Logger;

@Path("/package/{groupId}/{packageName}")
public class PackageResource {
  private String groupId;
  private String packageName;

  private static final Logger LOGGER = Logger.getLogger(PackageResource.class.getName());

  public PackageResource(@PathParam("groupId") String groupId, @PathParam("packageName") String packageName) {
    this.groupId = groupId;
    this.packageName = packageName;
  }

  @GET
  @Produces("text/html")
  public Viewable get() {

    PackageViewModel packageModel = new PackageViewModel(groupId, packageName);
    packageModel.setStatus(PackageDatabase.getStatus(groupId, packageName));

    if(packageModel.getStatus().isEmpty()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageModel.getLatestVersion()).get();
    VersionViewModel versionModel = new VersionViewModel(packageVersion);

    Map<String, Object> model = Maps.newHashMap();
    model.put("package", packageModel);
    model.put("version", versionModel);

    return new Viewable("/package.ftl", model);
  }

  @Path("{version}")
  public PackageVersionResource getVersion(@PathParam("version") String version) {
    PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, version);
    Optional<PackageVersion> packageVersion = PackageDatabase.getPackageVersion(packageVersionId);
    if(!packageVersion.isPresent()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return new PackageVersionResource(packageVersion.get());
  }

}
