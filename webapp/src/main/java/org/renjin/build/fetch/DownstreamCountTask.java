package org.renjin.build.fetch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.RPackageDependency;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import javax.ws.rs.POST;
import java.util.List;
import java.util.Set;

public class DownstreamCountTask {


  @POST
  public void calculate() {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    em.getTransaction().begin();
    List<RPackageVersion> packageVersions = em
      .createQuery("select distinct p from RPackageVersion p left join fetch p.reverseDependencies", RPackageVersion.class)
      .getResultList();

    for(RPackageVersion pkg : packageVersions) {
      pkg.setDownstreamCount(countDownstream(pkg));
    }

    em.getTransaction().commit();
  }

  @VisibleForTesting
  public static int countDownstream(RPackageVersion pkg) {
    return countDownstream(pkg, Sets.<String>newHashSet());
  }

  private static int countDownstream(RPackageVersion pkg, Set<String> visited) {

    System.out.println("Counting reverse deps for " + pkg.getPackage().getId());

    visited.add(pkg.getPackage().getId());

    int count = 0;
    for(RPackageDependency dep : pkg.getReverseDependencies()) {
      if(dep.getBuildScope().equals("compile") && !visited.contains(dep.getPackageVersion().getPackage().getId())) {
        count = count + 1 + countDownstream(dep.getPackageVersion(), visited);
      }
    }
    return count;
  }

  public static void main(String[] args) {
    new DownstreamCountTask().calculate();
  }

}
