package org.renjin.ci.admin;

import com.google.common.base.Optional;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.admin.migrate.ReIndexPackage;
import org.renjin.ci.admin.migrate.ReIndexPackageVersion;
import org.renjin.ci.admin.migrate.UpdatePubDates;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.index.GitHubTasks;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.pipelines.ForEachEntity;
import org.renjin.ci.pipelines.ForEachPackageVersion;
import org.renjin.ci.pipelines.Pipelines;
import org.renjin.ci.stats.StatPipelines;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.status;

@Path("/admin")
public class AdminResources {


  @GET
  @Produces(MediaType.TEXT_HTML)
  public Viewable get() {
    
    Map<String, Object> model = new HashMap<>();
    model.put("migrations", Arrays.asList(
        ReComputeBuildDeltas.class,
        ReIndexPackageVersion.class,
        UpdatePubDates.class,
        ReIndexPackage.class));
    
    return new Viewable("/admin.ftl", model);
  }
  
  @POST
  @Path("rebuildExamples")
  public Response rebuildExamples() {
    return Pipelines.redirectToStatus(Pipelines.forEach(new ExamplesExtractor()));
  }

  @POST
  @Path("migrate")
  public Response migrateEntity(@FormParam("functorClass") String functorClassName) {

    Class<?> functorClass;
    try {
      functorClass = Class.forName(functorClassName);
    } catch (ClassNotFoundException e) {
      throw new WebApplicationException(
           status(Response.Status.BAD_REQUEST)
          .entity("Class not found: " + functorClassName)
          .build());
    }

    Object functor;
    try {
      functor = functorClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    String jobId;
    if(functor instanceof ForEachEntity) {
      jobId = Pipelines.applyAll((ForEachEntity) functor);
    } else if(functor instanceof ForEachPackageVersion) {
      jobId = Pipelines.forEach((ForEachPackageVersion) functor);
    } else {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
      .entity("Unsupported functor class: " + functor.getClass().getName()).build());
    }
    
    return Pipelines.redirectToStatus(jobId);
  }

  @POST
  @Path("updateDeltaCounts")
  public Response updateBuildDeltaCounts() {
    return Pipelines.redirectToStatus(StatPipelines.startUpdateBuildStats());
  }
  
  
  @POST
  @Path("addGitHubRepo")
  public Response addGitHubRepo(@FormParam("repo") String repoId) {
    
    String[] repoParts = repoId.split("/");
    if(repoParts.length != 2) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Malformed github repo name").build();
    }

    Optional<Package> cranPackage = PackageDatabase.getPackageIfExists(new PackageId("org.renjin.cran", repoParts[1]));
    if(cranPackage.isPresent()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("CRAN package with name '" + repoParts[1] + "' already exists.").build();
    }
    
    PackageId packageId = new PackageId("org.renjin.github." + repoParts[0], repoParts[1]);
    
    Optional<Package> gitHubPackage = PackageDatabase.getPackageIfExists(packageId);
    if(gitHubPackage.isPresent()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Package " + packageId + " already exists.").build();
    }

    GitHubTasks.enqueue(repoParts[0], repoParts[1]);
    
    return Response.ok().entity("Enqueued.").build();
    
  }
}
