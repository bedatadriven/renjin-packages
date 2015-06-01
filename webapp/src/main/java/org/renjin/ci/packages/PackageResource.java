package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.googlecode.objectify.ObjectifyService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/package/{groupId}/{packageName}")
public class PackageResource {
  private String groupId;
  private String packageName;
  private final PackageId packageId;

  private static final Logger LOGGER = Logger.getLogger(PackageResource.class.getName());

  public PackageResource(@PathParam("groupId") String groupId, @PathParam("packageName") String packageName) {
    this.groupId = groupId;
    this.packageName = packageName;
    this.packageId = new PackageId(groupId, packageName);
  }

  @GET
  @Produces("text/html")
  public Viewable get() {

    org.renjin.ci.datastore.Package packageEntity = ObjectifyService.ofy().load().key(Package.key(packageId)).now();
    if(packageEntity == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    
    return getVersion(packageEntity.getLatestVersion()).getPage();
  }

  @Path("{version: [0-9][0-9\\-\\._A-Za-z]*}")
  public PackageVersionResource getVersion(@PathParam("version") String version) {
    PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, version);
    Optional<PackageVersion> packageVersion = PackageDatabase.getPackageVersion(packageVersionId);
    if(!packageVersion.isPresent()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return new PackageVersionResource(packageVersion.get());
  }
  
  @GET
  @Path("tests")
  @Produces(MediaType.APPLICATION_JSON)
  public Iterable<PackageTestResult> getTestResults() {
    return PackageDatabase.getTestResults(packageId).iterable();
  }

}
