package org.renjin.repo;


import com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class PackageResources {



  @GET
  @Path("index.html")
  public Viewable getIndex() {

    EntityManager em = HibernateUtil.getEntityManager();
    BuildResultDAO dao = new BuildResultDAO(em);

    Map<String, Object> model = Maps.newHashMap();
    model.put("buildResults", dao.queryResults(13));

    return new Viewable("/index.ftl", model);

  }
}
