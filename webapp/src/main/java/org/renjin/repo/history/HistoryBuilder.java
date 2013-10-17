package org.renjin.repo.history;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.renjin.repo.HibernateUtil;
import org.renjin.repo.model.RenjinCommit;
import org.renjin.repo.model.TestResult;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

public class HistoryBuilder {

  private final EntityManager em = HibernateUtil.getActiveEntityManager();
  private List<HistoryEvent> events = Lists.newArrayList();
  private Map<String, Boolean> ancestorsHaveTests = Maps.newHashMap();

  /**
   * Map Commit ID to test results
   */
  private Multimap<String, TestResult> commitResults = HashMultimap.create();

  public HistoryBuilder(TestResult latestResult) {

    List<TestResult> results = em.createQuery(
      "select r from TestResult r " + // left join fetch r.buildResult.build.renjinCommit " +
        "where r.test = :test", TestResult.class)
      .setParameter("test", latestResult.getTest())
      .getResultList();

    for(TestResult historicalResult : results) {
      commitResults.put(historicalResult.getBuildResult().getBuild().getRenjinCommit().getId(),
          historicalResult);
    }

    // fetch all commits into the session to avoid
    // billions of queries in the next step
    em.createQuery("select distinct c from RenjinCommit c " +
      "left join fetch c.parents").getResultList();

    // walk up the tree until we find a success.

    boolean passing = latestResult.isPassed();

    RenjinCommit head = latestResult.getBuildResult().getBuild().getRenjinCommit();
    while(head != null) {

      HistoryEvent event = new HistoryEvent(head, commitResults.get(head.getId()));
      if(event.hasTestResults()) {
        passing = event.getLatestTestResult().isPassed();
      }
      event.setPassing(passing);
      events.add(event);

      head = chooseParent(head);
    }
  }

  public List<HistoryEvent> getEvents() {
    return events;
  }

  private RenjinCommit chooseParent(RenjinCommit head) {
    for(RenjinCommit parent : head.getParents()) {
      if(ancestorHasTests(parent)) {
        return parent;
      }
    }
    return null;
  }

  private boolean ancestorHasTests(RenjinCommit commit) {
    if(commitResults.containsKey(commit.getId())) {
      return true;
    }
    // checked for cached answer
    if(ancestorsHaveTests.containsKey(commit.getId())) {
      return ancestorsHaveTests.get(commit.getId());
    }
    for(RenjinCommit parent : commit.getParents()) {
      if(ancestorHasTests(parent)) {
        ancestorsHaveTests.put(commit.getId(), true);
        return true;
      }
    }
    ancestorsHaveTests.put(commit.getId(), false);
    return false;
  }

}
