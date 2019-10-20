package org.renjin.ci.gradle.graph;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds a graph of packages and their dependencies
 */
public class PackageGraphBuilder {

  private static final Logger LOGGER = Logger.getLogger(PackageGraphBuilder.class.getName());

  private final ExecutorService executorService;
  private final DependencyCache dependencyCache;
  private final ReplacedPackageProvider replacedPackages;

  private final Map<PackageId, PackageNode> nodes = new HashMap<>();

  public PackageGraphBuilder(ExecutorService executorService, DependencyCache dependencyCache, ReplacedPackageProvider replacedPackages) {
    this.executorService = executorService;
    this.dependencyCache = dependencyCache;
    this.replacedPackages = replacedPackages;
  }

  public void add(String filter) throws InterruptedException {
    add(filter, null);
  }

  public void add(String filter, Double sample) throws InterruptedException {
    List<PackageVersionId> sampled = queryList(filter, sample);

    LOGGER.info("Building dependency graph...");

    for (PackageVersionId packageVersionId : sampled) {
      LOGGER.fine(packageVersionId.toString());
      add(packageVersionId);
    }
  }

  public static List<PackageVersionId> queryList(String filter, Double sample) {

    if(filter.contains(":")) {
      // consider as packageId
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(filter);
      return Collections.singletonList(packageVersionId);
    }

    LOGGER.info(String.format("Querying list of '%s' packages...", filter));
    List<PackageVersionId> packageVersionIds = RenjinCiClient.queryPackageList(filter);
    LOGGER.info(String.format("Found %d packages.", packageVersionIds.size()));

    return sample(packageVersionIds, sample);
  }

  /**
   * Samples a proportion or a fixed sample size of packages to build, generally for testing purposes.
   */
  private static List<PackageVersionId> sample(List<PackageVersionId> packageVersionIds, Double sample) {
    if(sample == null) {
      return packageVersionIds;
    } else {
      double fraction;
      if(sample > 1) {
        LOGGER.info(String.format("Sampling %.0f packages randomly", sample));
        fraction = sample / (double)packageVersionIds.size();
      } else {
        fraction = sample;
        LOGGER.info(String.format("Sampling %7.6f of packages randomly", sample));
      }

      List<PackageVersionId> sampled = new ArrayList<PackageVersionId>();
      Random random = new Random();
      for (PackageVersionId packageVersionId : packageVersionIds) {
        if(random.nextDouble() < fraction) {
          sampled.add(packageVersionId);
        }
      }
      LOGGER.info(String.format("Sampled %d packages", sampled.size()));

      return sampled;
    }
  }

  /**
   * Creates a packageNode for a specific packageVersion, and queues it for dependency resolution
   */
  private void add(PackageVersionId packageVersionId) {

    Preconditions.checkState(!nodes.containsKey(packageVersionId.getPackageId()),
        "%s has already been added to the graph.", packageVersionId);

    PackageNode node = new PackageNode(packageVersionId, resolveDependencies(packageVersionId));
    nodes.put(node.getId().getPackageId(), node);

  }

  private synchronized PackageNode getOrCreateNodeForDependency(ResolvedDependency resolvedDependency) {
    PackageVersionId pvid = resolvedDependency.getPackageVersionId();
    PackageNode node = nodes.get(pvid.getPackageId());
    if(node == null) {
      if(resolvedDependency.isReplaced() || replacedPackages.isReplaced(pvid)) {
        node = new PackageNode(resolvedDependency.getPackageVersionId(), Futures.immediateFuture(Collections.emptySet()));
        node.replaced(resolvedDependency.getReplacementVersion());
      } else {
        node = new PackageNode(pvid, resolveDependencies(resolvedDependency));
      }
      nodes.put(node.getId().getPackageId(), node);
    }
    return node;
  }


  private Future<Set<PackageNode>> resolveDependencies(ResolvedDependency resolvedDependency) {
    if(resolvedDependency.isReplaced()) {
      return Futures.immediateFuture(Collections.emptySet());
    } else {
      return resolveDependencies(resolvedDependency.getPackageVersionId());
    }
  }

  private Future<Set<PackageNode>> resolveDependencies(PackageVersionId pvid) {

    return executorService.submit(new Callable<Set<PackageNode>>() {
      @Override
      public Set<PackageNode> call() {

        LOGGER.info("Resolving " + pvid);

        ResolvedDependencySet resolution;

        resolution = dependencyCache.get(pvid);
        if(resolution == null) {
          try {
            resolution = RenjinCiClient.resolveDependencies(pvid);

            dependencyCache.cache(pvid, resolution);

          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to resolve dependencies of %s: %s", pvid, e.getMessage()), e);
            throw new RuntimeException("Failed to resolve dependency of " + pvid, e);
          }
        }

        Set<PackageNode> dependencies = new HashSet<>();

        for (ResolvedDependency resolvedDependency : resolution.getDependencies()) {
          if (resolvedDependency.isVersionResolved()) {
            dependencies.add(getOrCreateNodeForDependency(resolvedDependency));
          }
        }

        return dependencies;
      }
    });
  }

  public PackageGraph build() {

    Set<PackageNode> resolved = new HashSet<>();
    ArrayDeque<PackageNode> queue = new ArrayDeque<>(nodes.values());
    while(!queue.isEmpty()) {
      PackageNode packageNode = queue.pop();
      if(!resolved.contains(packageNode)) {
        queue.addAll(packageNode.getDependencies());
        resolved.add(packageNode);
      }
    }

    return new PackageGraph(nodes);
  }

}
