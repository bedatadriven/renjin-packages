package org.renjin.repo;


import com.google.common.base.Function;
import com.google.common.collect.*;
import org.renjin.repo.model.Build;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Predecessors {

  private final EntityManager em;
  private Map<String, Boolean> ancestorsHaveBuilds = Maps.newHashMap();

  /**
   * Map Commit ID to test results
   */
  private Multimap<String, Build> commitResults = HashMultimap.create();

  private List<Integer> predecessors = Lists.newArrayList();

  public Predecessors(EntityManager em, String commitId) {

    this.em = em;

    buildCommitToBuildMap();

    // fetch all commits into the session to avoid
    // billions of queries in the next step
    em.createQuery("select distinct c from RenjinCommit c " +
      "left join fetch c.parents").getResultList();


    RenjinCommit head = em.find(RenjinCommit.class, commitId);
    while(head != null) {
      List<Integer> buildIds = Lists.newArrayList();
      for(Build build : commitResults.get(head.getId())) {
        buildIds.add(build.getId());
      }
      if(!buildIds.isEmpty()) {
        Collections.sort(buildIds, Ordering.natural().reverse());
        predecessors.addAll(buildIds);
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

  public List<Integer> getPredecessors() {
    return predecessors;
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

}
