package org.renjin.ci;

import com.googlecode.objectify.Key;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.SystemRequirement;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("systemRequirement")
public class SystemRequirementResource {

  @GET
  @Path("{name}/version")
  public Response getLatestVersion(@PathParam("name") String name) {
    SystemRequirement systemRequirement = PackageDatabase.load().key(Key.create(SystemRequirement.class, name)).now();
    if(systemRequirement == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } else {
      return Response.ok().entity(systemRequirement.getVersion()).build();
    }
  }

  @POST
  public Response updateLatestVersion(@FormParam("name") String name, @FormParam("version") String version) {
    SystemRequirement requirement = new SystemRequirement();
    requirement.setName(name);
    requirement.setVersion(version);

    PackageDatabase.ofy().save().entity(requirement);

    return Response.ok().build();
  }
}
