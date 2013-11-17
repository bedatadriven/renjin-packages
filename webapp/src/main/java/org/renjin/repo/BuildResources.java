package org.renjin.repo;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RPackageBuildResult;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
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
  public Viewable getBuildSummary(@PathParam("id") int buildId, @QueryParam("compareTo") Integer comparisonBuildId) {
    Map<String, Object> model = Maps.newHashMap();

    EntityManager em = HibernateUtil.getActiveEntityManager();
    model.put("build", em.find(Build.class, buildId));
    model.put("totals",  queryTotals(buildId, em));
    model.put("blockers", em.createQuery("select p from RPackageBuildResult p where p.build.id = :buildId and p.succeeded = false " +
      "order by p.packageVersion.downstreamCount desc", RPackageBuildResult.class)
        .setParameter("buildId", buildId)
        .setMaxResults(15)
        .getResultList());

    if(comparisonBuildId != null) {
      model.put("reference", em.find(Build.class, comparisonBuildId));
      model.put("referenceTotals",  queryTotals(comparisonBuildId, em));
      model.put("regressions", querySucceedWhereFailed(buildId, comparisonBuildId, em));
      model.put("progressions", querySucceedWhereFailed(comparisonBuildId, buildId, em));
    }

    return new Viewable("/buildSummary.ftl", model);
  }

  private Map<String, Number> queryTotals(int buildId, EntityManager em) {
    List<Tuple> totals = em.createQuery("select p.outcome, count(*) from RPackageBuildResult p " +
      "where p.build.id = :buildId group by p.outcome", Tuple.class)
      .setParameter("buildId", buildId)
      .getResultList();

    Map<String, Number> totalMap = Maps.newHashMap();
    for(Tuple total : totals) {
      totalMap.put(((BuildOutcome)total.get(0)).name(), (Number)total.get(1));
    }
    return totalMap;
  }

  private List<RPackageBuildResult> querySucceedWhereFailed(int buildId, Integer comparisonBuildId, EntityManager em) {
    return em.createQuery("select p from RPackageBuildResult p where p.build.id = :buildId and " +
      "p.succeeded = false and p.packageVersion in (select o.packageVersion from RPackageBuildResult o where o.build.id=:reference and  " +
      "o.succeeded = true)", RPackageBuildResult.class)
      .setParameter("buildId", buildId)
      .setParameter("reference", comparisonBuildId)
      .getResultList();
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
