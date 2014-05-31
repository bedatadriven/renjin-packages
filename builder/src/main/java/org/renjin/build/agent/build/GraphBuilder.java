package org.renjin.build.agent.build;


import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.RPackageDependency;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

  /**
   * Maps version id to node
   */
  private Map<String, PackageNode> nodes = Maps.newHashMap();

  private Map<String, PackageNode> adding = Maps.newHashMap();


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
  public PackageNode addPackage(String name) throws IOException, UnresolvedDependencyException {
    EntityManager em = PersistenceUtil.createEntityManager();
    List<RPackageVersion> results = em.createQuery(
        "select v from RPackageVersion v where v.rPackage.name = :name and v.latest=true",
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

  private PackageNode addPackageVersion(RPackageVersion packageVersion) throws IOException, UnresolvedDependencyException {
    Preconditions.checkNotNull(packageVersion, "packageVersion");

    if(nodes.containsKey(packageVersion.getId())) {
      return nodes.get(packageVersion.getId());
    } else if(adding.containsKey(packageVersion.getId())) {
      System.out.println("(Circular Reference)");
      return adding.get(packageVersion.getId());

    } else {

      System.out.println("Adding node " + packageVersion.getId());
      PackageNode node = new PackageNode(packageVersion.getGroupId(),
        packageVersion.getPackageName(),
        packageVersion.getVersion());

      adding.put(node.getId(), node);

      // make sure we have all dependencies
      List<PackageEdge> edges = Lists.newArrayList();
      for(RPackageDependency dep : packageVersion.getDependencies()) {
        System.out.println("...linking " + dep.getDependencyName());
        if(dep.getDependency() == null) {
          throw new UnresolvedDependencyException(dep.getDependencyName());
        }
        if(!selfReferencing(dep)) {
          Preconditions.checkNotNull(dep.getDependency(), "dep.getDependency()");
          if(dep.getBuildScope().equals("compile")) {
            edges.add(new PackageEdge(addPackageVersion(dep.getDependency()), dep.getType()));
          } else {
            try {
              addPackageVersion(dep.getDependency());
            } catch(Exception e) {
              System.out.println("Warning: could not resolve test dependency " + dep.getDependencyName()
                + ": " + e.getMessage());
            }
          }
        }
      }

      // add the node
      node.getEdges().addAll(edges);
      nodes.put(node.getId(), node);
      adding.remove(node.getId());

      return node;
    }
  }

  private boolean selfReferencing(RPackageDependency dep) {
    if(dep == null) {
      return false;
    }
    if(dep.getDependency() == null) {
      return false;
    }
    return dep.getDependencyName().equals(dep.getDependencyVersion());
  }

  public void addAllLatestVersions() throws IOException {

    List<RPackageVersion> results = em.createQuery("SELECT DISTINCT v from RPackageVersion v " +
        "LEFT JOIN FETCH v.dependencies WHERE v.latest = true",
      RPackageVersion.class)
      .getResultList();

    for(RPackageVersion packageVersion : results) {
      try {
        addPackageVersion(packageVersion);
      } catch(UnresolvedDependencyException ude) {
        System.out.println(packageVersion + " not added because of unresolved dependency: " + ude.getPackageName());
      }
    }
  }

  public Map<String, PackageNode> build() {
    em.close();
    return nodes;
  }

}
