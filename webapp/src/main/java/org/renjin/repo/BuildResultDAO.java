package org.renjin.repo;


import org.renjin.repo.model.RPackageBuildResult;

import javax.persistence.EntityManager;
import java.util.List;

public class BuildResultDAO {

  private EntityManager em;

  public BuildResultDAO(EntityManager em) {
    this.em = em;
  }

  public  List<RPackageBuildResult> queryResults(int buildId) {
    return em.createQuery("select r from RPackageBuildResult r  where r.build.id = 13 " +
            " order by r.packageVersion.rPackage.name").getResultList();

  }


}
