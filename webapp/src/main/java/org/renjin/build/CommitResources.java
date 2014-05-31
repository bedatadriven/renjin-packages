package org.renjin.build;


import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.view.Viewable;
import org.renjin.build.history.HistoryBuilder;
import org.renjin.build.model.RenjinCommit;
import org.renjin.build.model.TestResult;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

public class CommitResources {

  @GET
  public Viewable getCommitIndex() {

    EntityManager em = HibernateUtil.getActiveEntityManager();

    // get deltas per build
    List<Object[]> buildStats = em.createNativeQuery(
        "select b.renjinCommitId commit, r.build_id buildId, sum(r.delta>0) plus, sum(r.delta<0) minus from RPackageBuildResult r " +
        "left join Build b on (b.id=r.build_id) group by r.build_id")
      .getResultList();

    Multimap<String, BuildViewModel> buildStatsByCommit = HashMultimap.create();
    for(Object[] build : buildStats) {
      buildStatsByCommit.put((String)build[0],
        new BuildViewModel((Integer) build[1],
          (Number)build[2],
          (Number)build[3]));
    }

    List<CommitViewModel> models = Lists.newArrayList();

    // query all the commits at once
    List<RenjinCommit> commits = em.createQuery("select c from RenjinCommit c order by c.commitTime desc", RenjinCommit.class).getResultList();
    for(RenjinCommit commit : commits) {
      models.add(new CommitViewModel(commit, buildStatsByCommit.get(commit.getId())));
    }

    Map<String, Object> model = Maps.newHashMap();
    model.put("commits", models);

    return new Viewable("/commitIndex.ftl", model);
  }

  @GET
  @Path("{sha}")
  public Viewable getProgress(@PathParam("sha") String sha, @QueryParam("compareTo") String releaseVersion) {

    if(Strings.isNullOrEmpty(releaseVersion)) {
      releaseVersion = "0.7.0-RC5";
    }

    EntityManager em = HibernateUtil.getActiveEntityManager();
    RenjinCommit commit = em.find(RenjinCommit.class, sha);
    RenjinCommit releaseCommit = em.createQuery("select c from RenjinCommit c where c.version = :release", RenjinCommit.class)
        .setParameter("release", releaseVersion)
        .getSingleResult();

    Map<String, Object> model = Maps.newHashMap();
    model.put("commit", commit);
    model.put("totals", queryTotals(em, commit));
    model.put("referenceTotals", queryTotals(em, releaseCommit));
    model.put("regressions", queryRegressions(em, releaseCommit, commit));

    return new Viewable("/commitSummary.ftl", model);
  }

  private Tuple queryTotals(EntityManager em, RenjinCommit commit) {
    return em.createQuery("select count(*) as count, sum(case when(r.passed=true) then 1 else 0 end) as passingCount from TestResult r where r.renjinCommit = :commit", Tuple.class)
      .setParameter("commit", commit)
      .getSingleResult();
  }

  //create index TestResultCommitPassed on TestResult (renjinCommitId, passed) using hash

  private List<Tuple> queryRegressions(EntityManager em, RenjinCommit from, RenjinCommit to) {
    return em.createQuery("select tr.test.name as testName, tr.test.id as testId, tr.errorMessage as errorMessage, tr.packageVersion.id as packageVersionId from TestResult tr " +
        "where tr.renjinCommit = :to and tr.passed = false and tr.test in" +
        " (select br.test from TestResult br where br.renjinCommit = :from and br.passed=true)", Tuple.class)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList();

  }

  @GET
  @Path("{sha}/tests/{testId}")
  public Viewable getTestHistory(@PathParam("sha") String sha, @PathParam("testId") int testId) {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    TestResult result = em.createQuery("select r from TestResult r where r.test.id = :testId and " +
      "r.renjinCommit.id = :sha", TestResult.class)
      .setParameter("testId", testId)
      .setParameter("sha", sha)
      .getSingleResult();

    Map<String, Object> model = Maps.newHashMap();
    model.put("test", result.getTest());
    model.put("events", new HistoryBuilder(result).getEvents());

    return new Viewable("/commitTestHistory.ftl", model);
  }
}
