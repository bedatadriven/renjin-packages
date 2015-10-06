package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Optional;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    Package packageEntity = loadPackage();
    if(packageEntity.isReplaced()) {
      return getReplacementPage(packageEntity);
    } else {
      return getVersion(packageEntity.getLatestVersion()).getPage();
    }
  }

  private Viewable getReplacementPage(Package packageEntity) {
    
    
    Map<String, Object> model = new HashMap<>();
    model.put("package", packageEntity);
    model.put("replacement", new ReplacementVersionPage(packageEntity));
    
    return new Viewable("/packageReplacement.ftl", model);
  }

  private Package loadPackage() {
    Package packageEntity = ObjectifyService.ofy().load().key(Package.key(packageId)).now();
    if(packageEntity == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return packageEntity;
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

  /**
   * Called by Jenkins when a pure-Renjin replacement package is built.
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Path("/replacement")
  public Response createRelease(@FormParam("version") final String version) {
    
    if(Strings.isNullOrEmpty(version)) {
      throw new WebApplicationException(Response
          .status(Response.Status.BAD_REQUEST)
          .entity("version form parameter required").build());
    }
    
    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        Package pkg = loadPackage();
        pkg.setReplaced(true);
        pkg.setLatestReplacementVersion(version);

        ReplacementRelease release = new ReplacementRelease(packageId, version);
        release.setReleaseDate(new Date());
        
        ObjectifyService.ofy().save().entities(pkg, release);
      }
    });
    
    return Response.ok().build();
  }
  
  @GET
  @Path("tests")
  @Produces(MediaType.APPLICATION_JSON)
  public Iterable<PackageTestResult> getTestResults() {
    return PackageDatabase.getTestResults(packageId).iterable();
  }

}
