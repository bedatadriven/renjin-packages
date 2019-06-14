package org.renjin.ci.gradle.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.ResolvedDependencySet;

import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds a graph of packages and their dependencies
 */
public class PackageGraphBuilder {

  private static final Logger LOGGER = Logger.getLogger(PackageGraphBuilder.class.getName());

  private boolean rebuildFailedDependencies;
  private boolean rebuildAllDependencies;

  private final Map<PackageVersionId, PackageNode> nodes = new HashMap<PackageVersionId, PackageNode>();

  /**
   * Queue of packages for which we must resolve dependencies
   */
  private final Map<PackageNode, Future<ResolvedDependencySet>> toResolve = Maps.newHashMap();


  public PackageGraphBuilder(boolean rebuildFailedDependencies, boolean rebuildAllDependencies) {
    this.rebuildFailedDependencies = rebuildFailedDependencies;
    this.rebuildAllDependencies = rebuildAllDependencies;
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
  private void add(PackageVersionId packageVersionId) throws InterruptedException {

    Preconditions.checkState(!nodes.containsKey(packageVersionId),
        "%s has already been added to the graph.", packageVersionId);

    PackageNode node = new PackageNode(packageVersionId);
    nodes.put(node.getId(), node);

    scheduleDependencyResolution(node);
  }

  private void scheduleDependencyResolution(PackageNode packageNode) {
    // is resolution already in progress?
    if(toResolve.containsKey(packageNode)) {
      return;
    }
    
    // otherwise, schedule 
  }

  private PackageNode getOrCreateNodeForDependency(ResolvedDependency resolvedDependency) throws InterruptedException {
    PackageVersionId pvid = resolvedDependency.getPackageVersionId();
    PackageNode node = nodes.get(pvid);
    if(node == null) {
      node = new PackageNode(pvid);
      nodes.put(node.getId(), node);

      // Add dependencies

      if(resolvedDependency.isReplaced()) {
        // There is a java replacement version for this library 
        node.replaced(resolvedDependency.getReplacementVersion());
     
      } else if(resolvedDependency.hasBuild()) {
        
        if(shouldRebuild(resolvedDependency)) {
          // attempt to rebuild this failed dependency
          resolveDependencies(node);
        } else {
          // mark it as already provided.
          node.provideBuild(resolvedDependency.getBuildNumber(), resolvedDependency.getBuildOutcome());
        }
      } else {
        // We will need to build this one as well...
        resolveDependencies(node);
      }
    }
    return node;
  }

  private boolean shouldRebuild(ResolvedDependency resolvedDependency) {
    if(rebuildAllDependencies) {
      return true;
    }
    if(rebuildFailedDependencies) {
      return resolvedDependency.getBuildOutcome() != BuildOutcome.SUCCESS;
    }
    return false;
  }

  private void resolveDependencies(PackageNode node) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    LOGGER.info(String.format("Resolving dependencies of %s...", node.getId()));

    ResolvedDependencySet resolution;
    try {
      resolution = RenjinCiClient.resolveDependencies(node.getId());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, String.format("Failed to resolve dependencies of %s: %s", node.getId(), e.getMessage()), e);
      node.orphan();
      return;
    }

    for (ResolvedDependency resolvedDependency : resolution.getDependencies()) {
      if(resolvedDependency.isVersionResolved()) {
        node.dependsOn(getOrCreateNodeForDependency(resolvedDependency));
      } else {
        node.addUnresolvedDependency(resolvedDependency.getName());
      }
    }
  }

  public PackageGraph build() throws Exception {

    /*
     * For all packages that we WANT to build, determine their dependencies and either resolve
     * to an existing build or schedule a new one.
     */
    List<PackageNode> toResolve = Lists.newArrayList(nodes.values());
    for (PackageNode packageNode : toResolve) {
      resolveDependencies(packageNode);
    }

    return new PackageGraph(nodes);
  }

}
