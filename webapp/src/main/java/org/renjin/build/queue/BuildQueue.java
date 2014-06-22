package org.renjin.build.queue;


import com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.BuildStage;
import org.renjin.build.model.RPackageBuild;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

public class BuildQueue {


  @GET
  @Path("dashboard")
  public Viewable getDashboard() {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    Map<String, Object> pageModel = Maps.newHashMap();

    pageModel.put("commits", em.createQuery("select c from RenjinCommit c order by c.commitTime desc").getResultList());
    pageModel.put("active", stageModel(BuildStage.LEASED));
    pageModel.put("ready", stageModel(BuildStage.READY));
    pageModel.put("waiting", stageModel(BuildStage.WAITING));
    pageModel.put("completed", lastCompleted());

    return new Viewable("/buildQueue.ftl", pageModel);
  }

  private List<RPackageBuild> lastCompleted() {
    return HibernateUtil.getActiveEntityManager()
        .createQuery(
            "select b from RPackageBuild b " +
                "left join fetch b.packageVersion " +
                "where b.stage = 'completed' and b.completionTime is not null order by b.completionTime desc",
            RPackageBuild.class)
        .setMaxResults(10)
        .getResultList();
  }

  private Map<String, Object> stageModel(BuildStage stage) {
    Map<String, Object> model = Maps.newHashMap();
    model.put("top", queryHead(stage));
    model.put("count", countStage(stage));
    return model;
  }

  private int countStage(BuildStage stage) {
    Number count = (Number)HibernateUtil.getActiveEntityManager()
        .createQuery("select count(*) from RPackageBuild b where b.stage = :stage")
        .setParameter("stage", stage)
        .getSingleResult();

    return count.intValue();
  }

  private List<RPackageBuild> queryHead(BuildStage stage) {
    return HibernateUtil.getActiveEntityManager()
        .createQuery("select b from RPackageBuild b " +
            "left join fetch b.packageVersion " +
            "where b.stage = :stage", RPackageBuild.class)
        .setParameter("stage", stage)
        .setMaxResults(10)
        .getResultList();
  }

  @Path("cancelAll")
  @POST
  public Response cancelAll(@Context UriInfo uriInfo) {
    EntityManager em = HibernateUtil.getActiveEntityManager();
    em.getTransaction().begin();
    em.createNativeQuery("update RPackageBuild set stage='COMPLETE', outcome='CANCELLED' " +
        "where stage in ('READY', 'WAITING')");
    em.getTransaction().commit();

    return Response.temporaryRedirect(uriInfo.getRequestUriBuilder().replacePath("/queue/dashboard").build())
        .build();

  }

  @GET
  @Path("result/{id}")
  public Viewable getResult(int buildId) {
    RPackageBuild build = HibernateUtil.getActiveEntityManager().find(RPackageBuild.class, buildId);
    return new Viewable("/buildResult.ftl", build);
  }

  @Path("launch")
  public BuildLauncher launch() {
    return new BuildLauncher();
  }

  @Path("control")
  public BuildQueueController control() {
    return new BuildQueueController();
  }
}
