package org.renjin.cran;


import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.repo.model.RPackageDependency;
import org.renjin.repo.model.RPackageVersion;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

  /**
   * Maps version id to node
   */
  private Map<String, PackageNode> nodes = Maps.newHashMap();


  private EntityManager em;

  public GraphBuilder() {
    em = PersistenceUtil.createEntityManager();
  }

  /**
   * Adds an individual package by name
   * @param name
   * @return
   * @throws IOException
   */
  public PackageNode addPackage(String name) throws IOException {
    EntityManager em = PersistenceUtil.createEntityManager();
    List<RPackageVersion> results = em.createQuery("select v from RPackageVersion v where v.rPackage.name = :name and v.latest=true",
      RPackageVersion.class)
      .setParameter("name", name)
      .getResultList();

    if(results.size() == 0) {
      throw new IllegalArgumentException("Can't find package with name '" + name + "'");
    } else if(results.size() > 1) {
      throw new IllegalArgumentException("Ambiguous name '" + name + "', could be: " +
        Joiner.on(", ").join(Collections2.transform(results, new Function<RPackageVersion, Object>() {
          @Override
          public Object apply(RPackageVersion packageVersion) {
            return packageVersion.getId();
          }
        })));
    }

    return addPackageVersion(results.get(0));
  }

  private PackageNode addPackageVersion(RPackageVersion packageVersion) throws IOException {
    if(nodes.containsKey(packageVersion.getId())) {
      return nodes.get(packageVersion.getId());
    } else {

      System.out.println("Adding node " + packageVersion.getId());

      PackageNode node = new PackageNode(packageVersion.getGroupId(), packageVersion.getPackageName(),
        packageVersion.getVersion());
      nodes.put(node.getId(), node);

      // make sure we have all dependencies
      for(RPackageDependency dep : packageVersion.getDependencies()) {
        if(dep != null && !dep.getDependency().getPackageName().equals(packageVersion.getPackageName()) &&
            dep.getBuildScope().equals("compile")) {
          node.getEdges().add(new PackageEdge(addPackageVersion(dep.getDependency()), dep.getType()));
        }
      }

      return node;
    }
  }

  public void addAllLatestVersions() throws IOException {

    List<RPackageVersion> results = em.createQuery("SELECT DISTINCT v from RPackageVersion v " +
        "LEFT JOIN FETCH v.dependencies WHERE v.latest = true",
      RPackageVersion.class)
      .getResultList();

    for(RPackageVersion packageVersion : results) {
      addPackageVersion(packageVersion);
    }
  }

  public Map<String, PackageNode> build() {
    em.close();
    return nodes;
  }

}
