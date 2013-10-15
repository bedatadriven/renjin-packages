package org.renjin.repo;


import com.google.appengine.repackaged.com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.history.HistoryBuilder;
import org.renjin.repo.model.RenjinCommit;
import org.renjin.repo.model.Test;
import org.renjin.repo.model.TestResult;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.List;
import java.util.Map;

public class CommitResources {

  @GET
  public Viewable getCommitIndex() {

    List<RenjinCommit> versionList = HibernateUtil.getActiveEntityManager()
      .createQuery("select c from RenjinCommit c order by c.commitTime desc", RenjinCommit.class)
      .getResultList();

    Map<String, Object> model = Maps.newHashMap();
    model.put("commits", versionList);

    return new Viewable("/commitIndex.ftl", model);
  }

  @GET
  @Path("{sha}")
  public Viewable getCommitSummary(@PathParam("sha") String sha) {
    EntityManager em = HibernateUtil.getActiveEntityManager();

    Map<String, Object> model = Maps.newHashMap();
    model.put("commit", em.find(RenjinCommit.class, sha));


    List<TestResult> testResults = HibernateUtil.getActiveEntityManager()
      .createQuery("select r from TestResult r where r.buildResult.build.renjinCommit.id = :sha", TestResult.class)
      .setParameter("sha", sha)
      .getResultList();

    model.put("testResults", testResults);

    return new Viewable("/commitSummary.ftl", model);
  }

  @GET
  @Path("{sha}/tests/{testId}")
  public Viewable getTestHistory(@PathParam("sha") String sha, @PathParam("testId") int testId) {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    TestResult result = em.createQuery("select r from TestResult r where r.test.id = :testId and " +
      "r.buildResult.build.renjinCommit.id = :sha", TestResult.class)
      .setParameter("testId", testId)
      .setParameter("sha", sha)
      .getSingleResult();

    Map<String, Object> model = Maps.newHashMap();
    model.put("test", result.getTest());
    model.put("events", new HistoryBuilder(result).getEvents());

    return new Viewable("/commitTestHistory.ftl", model);
  }
}
