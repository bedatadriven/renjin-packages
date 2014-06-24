package org.renjin.build.queue;


import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.build.HibernateUtil;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.BuildStage;
import org.renjin.build.model.RPackageBuild;
import org.renjin.build.task.PackageBuildResult;
import org.renjin.build.task.PackageBuildTask;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildQueue {


  private static final Logger LOGGER = Logger.getLogger(BuildQueue.class.getName());

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
        "where stage in ('READY', 'WAITING')")
        .executeUpdate();
    em.getTransaction().commit();

    return Response.seeOther(uriInfo.getRequestUriBuilder().replacePath("/queue/dashboard").build())
        .build();

  }

  @Path("launch")
  public BuildLauncher launch() {
    return new BuildLauncher();
  }

  @Path("control")
  public BuildQueueController control() {
    return new BuildQueueController();
  }

  @POST
  @Path("lease")
  @Produces("application/json")
  public Response leaseTask(@FormParam("workerId") String workerId) {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
    try {
      List<RPackageBuild> resultList = em.createQuery(
          "select b from RPackageBuild b " +
              "where stage='READY'", RPackageBuild.class)
          .setMaxResults(1)
          .getResultList();

      RPackageBuild build;
      if(!resultList.isEmpty()) {
        build = resultList.get(0);
        build.setLeased(workerId);
        build.setLeaseTime(new Date());
        build.setStage(BuildStage.LEASED);
      } else {
        // none available
        return Response.status(Response.Status.NO_CONTENT).build();
      }
      em.getTransaction().commit();

      PackageBuildTask task = new PackageBuildTask();
      task.setPackageName(build.getPackageName());
      task.setPackageGroupId(build.getPackageVersion().getGroupId());
      task.setPackageVersion(build.getPackageVersion().getVersion());
      task.setBuildId(build.getBuild().getId());
      task.setRenjinVersion(build.getBuild().getRenjinCommit().getVersion());
      if(!Strings.isNullOrEmpty(build.getDependencyVersions())) {
        task.setDependencies(Arrays.asList(Strings.nullToEmpty(build.getDependencyVersions()).split(",")));
      } else {
        task.setDependencies(Collections.<String>emptyList());
      }

      return Response.ok(task, MediaType.APPLICATION_JSON_TYPE).build();

    } catch(Exception e) {
      em.getTransaction().rollback();
      LOGGER.log(Level.SEVERE, "Exception while leasing build...");
      throw e;

    } finally {
      try {
        em.close();
      } catch(Exception e) {
        LOGGER.log(Level.SEVERE, "Exception while closing EntityManager", e);
      }
    }
  }

  @POST
  @Path("result")
  @Consumes("application/json")
  public Response postResult(PackageBuildResult result) {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();

    RPackageBuild packageBuild = em.find(RPackageBuild.class, result.getId());
    if(Objects.equals(result.getWorkerId(), packageBuild.getLeased())) {
      packageBuild.setStage(BuildStage.COMPLETED);
      packageBuild.setCompletionTime(new Date());
      packageBuild.setOutcome(result.getOutcome());
      packageBuild.setNativeSourceCompilationFailures(result.isNativeSourcesCompilationFailures());
      em.persist(packageBuild);
    }

    em.getTransaction().commit();

    return Response.ok().build();
  }
}
