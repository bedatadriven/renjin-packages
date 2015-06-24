package org.renjin.ci.packages;

import org.glassfish.jersey.server.mvc.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Path("/")
public class RootResource {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Viewable get() {
    return new PackageListResource().getIndex();
  }


  @GET
  @Path("/index.html")
  public Response getIndexHtml(@Context UriInfo uriInfo) {
    return Response
      .status(Response.Status.MOVED_PERMANENTLY)
        .location(uriInfo.getAbsolutePathBuilder().build()).build();
  }
  

}
