package org.renjin.repo;


import com.sun.jersey.api.view.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.HashMap;

@Path("/")
public class PackageResources {



  @GET
  @Path("index.html")
  public Viewable getIndex() {


    return new Viewable("index", new HashMap());

  }
}
