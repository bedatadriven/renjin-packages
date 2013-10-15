package org.renjin.repo;

import com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.RPackageBuildResult;

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

  @GET
  @Path("{buildId}/{groupId}/{artifactId}/{version}")
  public Viewable getBuildResult(@PathParam("buildId") int buildId,
                                  @PathParam("groupId") String groupId,
                                  @PathParam("artifactId") String artifactId,
                                  @PathParam("version") String version) {


    EntityManager em = HibernateUtil.getActiveEntityManager();
    RPackageBuildResult result = em.createQuery("select r from RPackageBuildResult r " +
            "where r.build.id=:buildId and " +
            "packageVersion.id = :gav", RPackageBuildResult.class)
            .setParameter("buildId", buildId)
            .setParameter("gav", groupId + ":" + artifactId + ":" + version)
            .getSingleResult();

    return new Viewable("/buildResult.ftl", result);
  }
  
}
