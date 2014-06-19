package org.renjin.build;


import org.renjin.build.model.RPackageBuild;

import javax.persistence.EntityManager;
import java.util.List;

public class BuildResultDAO {

  private EntityManager em;

  public BuildResultDAO(EntityManager em) {
    this.em = em;
  }

  public  List<RPackageBuild> queryResults(int buildId) {
    return em.createQuery("select r from RPackageBuild r  where r.build.id = 13 " +
            " order by r.packageVersion.rPackage.name").getResultList();

  }
}
