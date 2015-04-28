package org.renjin.ci.packages;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;
import org.renjin.ci.packages.results.PackageResults;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    PackageViewModel packageModel = new PackageViewModel(groupId, packageName);
    packageModel.setVersions(PackageDatabase.getPackageVersions(packageId));
    packageModel.setTestRuns(PackageDatabase.getTestResults(packageId).iterable());
    
    if(packageModel.getVersions().isEmpty()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    // Fetch all builds of this package
    packageModel.setBuilds(PackageDatabase.getBuilds(packageId).list());

    PackageVersion packageVersion = packageModel.getLatestVersion();
    VersionViewModel versionModel = new VersionViewModel(packageVersion);

    PackageResults results = new PackageResults();
    results.build(packageId);
    
    Map<String, Object> model = Maps.newHashMap();
    model.put("package", packageModel);
    model.put("version", versionModel);
    model.put("results", results);

    return new Viewable("/package.ftl", model);
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
