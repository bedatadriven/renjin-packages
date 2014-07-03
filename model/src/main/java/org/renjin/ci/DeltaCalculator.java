package org.renjin.ci;


import com.google.common.collect.*;
import org.renjin.ci.model.Build;
import org.renjin.ci.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DeltaCalculator {

  private final EntityManager em;
  private final int buildId;

  private Map<String, Boolean> ancestorsHaveBuilds = Maps.newHashMap();

  /**
   * Map Commit ID to Builds
   */
  private Multimap<String, Build> commitResults = HashMultimap.create();

  private List<Integer> parentBuildIds = Lists.newArrayList();

  public DeltaCalculator(EntityManager em, int buildId) {

    this.em = em;
    this.buildId = buildId;
  }

  public void calculate() {
    makeParentBuildList();
    updateDeltaFlags();
  }

  private void makeParentBuildList() {

    buildCommitToBuildMap();

    // fetch all commits into the session to avoid
    // billions of queries in the next step
    em.createQuery("select distinct c from RenjinCommit c " +
      "left join fetch c.parents").getResultList();


    RenjinCommit head = em.find(RenjinCommit.class, em.find(Build.class, buildId).getRenjinCommit().getId());
    String headSha = head.getId();

    while(head != null) {
      List<Integer> buildIds = Lists.newArrayList();
      for(Build build : commitResults.get(head.getId())) {
        if(!build.getRenjinCommit().getId().equals(headSha) ||
           build.getId() < buildId) {

          buildIds.add(build.getId());
        }
      }
      if(!buildIds.isEmpty()) {
        Collections.sort(buildIds, Ordering.natural().reverse());
        parentBuildIds.addAll(buildIds);
      }

      head = chooseParent(head);
    }
  }

  private void buildCommitToBuildMap() {
    List<Build> builds = em.createQuery("select b from Build b", Build.class)
      .getResultList();

    for(Build build : builds) {
      commitResults.put(build.getRenjinCommit().getId(), build);
    }
  }

  private RenjinCommit chooseParent(RenjinCommit head) {
    for(RenjinCommit parent : head.getParents()) {
      if(ancestorHasBuilds(parent)) {
        return parent;
      }
    }
    return null;
  }

  private boolean ancestorHasBuilds(RenjinCommit commit) {
    if(commitResults.containsKey(commit.getId())) {
      return true;
    }
    // checked for cached answer
    if(ancestorsHaveBuilds.containsKey(commit.getId())) {
      return ancestorsHaveBuilds.get(commit.getId());
    }
    for(RenjinCommit parent : commit.getParents()) {
      if(ancestorHasBuilds(parent)) {
        ancestorsHaveBuilds.put(commit.getId(), true);
        return true;
      }
    }
    ancestorsHaveBuilds.put(commit.getId(), false);
    return false;
  }

  private void updateDeltaFlags() {

    em.getTransaction().begin();
    em.createQuery("UPDATE RPackageBuild r SET r.delta = NULL where r.build.id = :buildId")
      .setParameter("buildId", buildId)
      .executeUpdate();

    for(int parentBuildId : parentBuildIds) {
      setDelta(-1, parentBuildId, false, true);
      setDelta(+1, parentBuildId, true, false);
      setDelta( 0, parentBuildId, false, false);
      setDelta( 0, parentBuildId, true, true);
    }

    em.getTransaction().commit();


  }

  private void setDelta(int sign, int parentBuildId, boolean buildSucceeded, boolean parentSucceeded) {

    List<Integer> buildResultIds = queryChangeType(parentBuildId, buildSucceeded, parentSucceeded);

    if(!buildResultIds.isEmpty()) {

      em.createQuery("UPDATE RPackageBuild r SET r.delta = :deltaValue " +
                        "WHERE r.id in (:resultIds) and r.delta IS NULL")
          .setParameter("deltaValue", sign)
          .setParameter("resultIds", buildResultIds)
          .executeUpdate();
    }

  }

  private List<Integer> queryChangeType(int parentBuildId, boolean childSucceeded, boolean parentSucceeded) {

    String jpql = "SELECT r.id FROM RPackageBuild r WHERE r.build.id = :buildId and " +
      buildSucceeded("r.outcome", childSucceeded) + " and " +
      "r.packageVersion in (SELECT pr.packageVersion FROM RPackageBuild pr WHERE pr.build.id = :parentBuildId and " +
      buildSucceeded("pr.outcome", parentSucceeded) + ")";

    return em.createQuery(jpql, Integer.class)
      .setParameter("buildId", buildId)
      .setParameter("parentBuildId", parentBuildId)
      .getResultList();
  }

  private String buildSucceeded(String field, boolean succeeded) {
    return field + (succeeded ? " = " : "!=") + "org.renjin.repo.model.BuildOutcome.SUCCESS";
  }

}
