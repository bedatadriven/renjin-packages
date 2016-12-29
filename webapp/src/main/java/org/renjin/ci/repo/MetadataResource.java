package org.renjin.ci.repo;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;


public class MetadataResource {

  @GET
  public Response get() {
    return Response.status(404).build();
  }


}
