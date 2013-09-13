package org.renjin.repo.task;


import com.google.common.collect.Sets;
import org.renjin.repo.PersistenceUtil;
import org.renjin.repo.model.RPackage;
import org.renjin.repo.model.RPackageVersion;

import javax.persistence.EntityManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Set;

/**
 * Fetch the latest list of package versions from a CRAN mirror
 */
@Path("/tasks")
public class UpdateCranPackagesTask {


  @POST
  @Path("fetch")
  public void fetchList() {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
   
    Set<String> knownSet = Sets.newHashSet();
    List<String> packageVersionList = em.createQuery("select v.id from RPackageVersion v")
            .getResultList();
    for(String pkgVersionId : packageVersionList) {
      knownSet.add(pkgVersionId);
    }

    for(PackageEntry cranPackage : CRAN.fetchPackageList()) {
      String pkgId = "org.renjin.cran:" + cranPackage.getName();
      String pkgVersionId = pkgId + ":" + cranPackage.getVersion();
      if(!knownSet.contains(pkgVersionId)) {
        RPackage pkg = em.find(RPackage.class, pkgId);
        if(pkg == null) {
          System.out.println("Registering new package " + pkgId);

          pkg = new RPackage();
          pkg.setId(pkgId);
          pkg.setName(cranPackage.getName());
          em.persist(pkg);
        }

        System.out.println("Registering new package version " + pkgVersionId);

        RPackageVersion pkgVersion = new RPackageVersion();
        pkgVersion.setId(pkgVersionId);
        pkgVersion.setRPackage(pkg);
        pkgVersion.setVersion(cranPackage.getVersion());
        em.persist(pkgVersion);
      }
    }

    em.getTransaction().commit();
    em.close();
  }

}
