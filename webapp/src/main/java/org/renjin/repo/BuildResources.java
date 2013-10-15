package org.renjin.repo;

import com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.model.Build;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

public class BuildResources {

  @GET
  public Viewable getIndex() {

    Map<String, Object> model = Maps.newHashMap();

    EntityManager em = HibernateUtil.getActiveEntityManager();
    model.put("builds",
      em.createQuery("select b from Build b order by b.started desc")
        .getResultList());

    return new Viewable("/buildIndex.ftl", model);
  }

  @GET
  @Path("{id}")
  public Viewable getBuildSummary(@PathParam("id") int buildId) {
    Map<String, Object> model = Maps.newHashMap();

    EntityManager em = HibernateUtil.getActiveEntityManager();
    model.put("build", em.find(Build.class, buildId));

    return new Viewable("/buildSummary.ftl", model);

  }

}
