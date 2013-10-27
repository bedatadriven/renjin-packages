package org.renjin.repo;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.RPackageBuildResult;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
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

  @Path("{buildId}/{groupId}/{artifactId}/{version}")
  public ResultResource getBuildResult(@PathParam("buildId") int buildId,
                                       @PathParam("groupId") String groupId,
                                       @PathParam("artifactId") String artifactId,
                                       @PathParam("version") String version) {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    List<RPackageBuildResult> results = em.createQuery("select r from RPackageBuildResult r " +
            "where r.build.id=:buildId and " +
            "packageVersion.id = :gav", RPackageBuildResult.class)
            .setParameter("buildId", buildId)
            .setParameter("gav", groupId + ":" + artifactId + ":" + version)
            .getResultList();

    if(results.isEmpty()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    if(results.size() > 1) {
      Collections.sort(results, Ordering.natural().onResultOf(new Function<RPackageBuildResult, Comparable>() {
        @Override
        public Comparable apply(RPackageBuildResult result) {
          return result.getId();
        }
      }).reverse());
    }

    return new ResultResource(results.get(0));
  }
  
}
