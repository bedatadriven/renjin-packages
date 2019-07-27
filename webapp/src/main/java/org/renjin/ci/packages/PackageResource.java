package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.NoRobots;
import org.renjin.ci.NoRobotsFilter;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.qa.DisableVersionsPage;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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
      return getVersion(packageEntity.getLatestVersion()).getPage(true);
    }
  }

  @GET
  @Produces("text/plain")
  public String getLastGoodBuild() {
    String latestVersion = loadPackage().getLatestVersion();
    return latestVersion;
  }

  @GET
  @Path("badge")
  @Produces("image/svg+xml")
  public Viewable getRenjinIcon() {
    Package packageEntity = loadPackage();
    if(packageEntity.isReplaced()) {
      Map<String, Object> model = new HashMap<>();
      model.put("status", "Available");
    } else {

    }
    return new Viewable("/icon.ftl");
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
  public PackageVersionResource getVersion(@HeaderParam("user-agent") String userAgent, @PathParam("version") String version) {
    NoRobotsFilter.checkNotRobot(userAgent);
    return getVersion(version);
  }

  @VisibleForTesting
  PackageVersionResource getVersion(String version) {
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
  @NoRobots
  @Produces("text/html")
  @Path("disabled")
  public Viewable getDisableForm(@Context UriInfo uriInfo,
                                      @QueryParam("from") String fromVersion) throws URISyntaxException {

    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserLoggedIn()) {
      String loginUrl = userService.createLoginURL(uriInfo.getRequestUri().toString());
      throw new WebApplicationException(Response.seeOther(new URI(loginUrl)).build());
    }
    if(!userService.isUserAdmin()) {
      throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
    }
    
    DisableVersionsPage page = new DisableVersionsPage(packageId);
  
    if(!Strings.isNullOrEmpty(fromVersion)) {
      PackageVersionId fromVersionId = new PackageVersionId(packageId, fromVersion);
      for (PackageVersion packageVersion : page.getVersions()) {
        if(packageVersion.getPackageVersionId().compareTo(fromVersionId) <= 0) {
          packageVersion.setDisabled(true);
        }
      }
    }
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/disablePackageVersion.ftl", model);
  }
  
  @POST
  @Path("disabled")
  @Consumes("application/x-www-form-urlencoded")
  public Response post(@Context UriInfo uriInfo, MultivaluedMap<String, String> params) {

    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserAdmin()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    
    final String reason = params.getFirst("reason");
    final Set<String> disabledVersions = new HashSet<>();
    
    for (String paramName : params.keySet()) {
      if(!paramName.equals("reason")) {
        String version = paramName;
        boolean disabled = "on".equals(params.getFirst(version));
        if(disabled) {
          disabledVersions.add(version);
        }
      }
    }

    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {

        List<PackageVersion> toSave = new ArrayList<>();
        List<Key<PackageVersionDelta>> deltas = new ArrayList<>();
        List<PackageVersion> versions = PackageDatabase.getPackageVersions(packageId);

        for (PackageVersion version : versions) {
          boolean disabled = disabledVersions.contains(version.getPackageVersionId().getVersionString());
          if(version.isDisabled() != disabled) {
            version.setDisabled(disabled);
            if(disabled) {
              version.setDisabledReason(reason);
              deltas.add(PackageVersionDelta.key(version.getPackageVersionId()));
            } else {
              version.setDisabledReason(null);
            }
            toSave.add(version);
          }
        }
        
        ObjectifyService.ofy().save().entities(toSave);
      }
    });
    
    return Response.seeOther(uriInfo.getBaseUriBuilder().path(packageId.getPath()).build()).build();
  }

  @GET
  @Path("tests")
  @Produces(MediaType.APPLICATION_JSON)
  public Iterable<PackageTestResult> getTestResults() {
    return PackageDatabase.getTestResults(packageId).iterable();
  }

}
