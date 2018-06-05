package org.renjin.ci.packages;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class RootResource {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response get() {
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
