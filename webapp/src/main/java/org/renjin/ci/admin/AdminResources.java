package org.renjin.ci.admin;

import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.FixBuildKeys;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.admin.migrate.ReIndexBuild;
import org.renjin.ci.pipelines.ForEachEntity;
import org.renjin.ci.pipelines.ForEachPackageVersion;
import org.renjin.ci.pipelines.Pipelines;

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
    model.put("migrations", Arrays.asList(FixBuildKeys.class, ReComputeBuildDeltas.class, ReIndexBuild.class));
    
    return new Viewable("/admin.ftl", model);
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
}
