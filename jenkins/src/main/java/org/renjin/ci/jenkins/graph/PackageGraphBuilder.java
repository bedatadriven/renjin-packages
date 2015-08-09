package org.renjin.ci.jenkins.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.model.TaskListener;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.ResolvedDependencySet;

import java.util.*;
import java.util.concurrent.Future;

import static java.lang.String.format;

public class PackageGraphBuilder {

  private TaskListener taskListener;

  private final Map<PackageVersionId, PackageNode> nodes = new HashMap<PackageVersionId, PackageNode>();

  /**
   * Queue of packages for which we must resolve dependencies
   */
  private final Map<PackageNode, Future<ResolvedDependencySet>> toResolve = Maps.newHashMap();


  public PackageGraphBuilder(TaskListener taskListener) {
    this.taskListener = taskListener;
  }


  public PackageGraph build(String filter, Double sample) throws Exception {
    return build(filter, Collections.<String, String>emptyMap(), sample);
  }

  public PackageGraph build(String filter, Map<String, String> filterParameters, Double sample) throws Exception {

    /*
     * Step 1: Enqueue all packages that MUST be (re)built, even if there is an existing built
     */
    if(filter.contains(":")) {
      // consider as packageId
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(filter);
      enqueueForBuild(packageVersionId);
    } else {
      enqueueForBuild(filter, filterParameters, sample);
    }

    /*
     * Step 2: For all packages that we WANT to build, determine their dependencies and either resolve
     * to an existing build or schedule a new one.
     */
    List<PackageNode> toResolve = Lists.newArrayList(nodes.values());
    for (PackageNode packageNode : toResolve) {
      resolveDependencies(packageNode);
    }

    taskListener.getLogger().printf("Dependency graph built with %d nodes.\n", nodes.size());

    for (PackageNode packageNode : nodes.values()) {
      packageNode.computeDownstream();
    }

    return new PackageGraph(nodes);
  }

  private void enqueueForBuild(String filter, Map<String, String> filterParameters, Double sample) throws InterruptedException {
    taskListener.getLogger().println(format("Querying list of '%s' packages...\n", filter));
    List<PackageVersionId> packageVersionIds = RenjinCiClient.queryPackageList(filter);
    taskListener.getLogger().printf("Found %d packages.\n", packageVersionIds.size());

    List<PackageVersionId> sampled = sample(packageVersionIds, sample);

    taskListener.getLogger().println("Building dependency graph...");
    taskListener.getLogger().flush();

    for (PackageVersionId packageVersionId : sampled) {
      taskListener.getLogger().println(packageVersionId);
      enqueueForBuild(packageVersionId);
    }
  }

  private List<PackageVersionId> sample(List<PackageVersionId> packageVersionIds, Double sample) {
    if(sample == null) {
      return packageVersionIds;
    } else {
      double fraction;
      if(sample > 1) {
        taskListener.getLogger().println(format("Sampling %.0f packages randomly", sample));
        fraction = sample / (double)packageVersionIds.size();
      } else {
        fraction = sample;
        taskListener.getLogger().println(format("Sampling %7.6f of packages randomly", sample));
      }

      List<PackageVersionId> sampled = new ArrayList<PackageVersionId>();
      Random random = new Random();
      for (PackageVersionId packageVersionId : packageVersionIds) {
        if(random.nextDouble() < fraction) {
          sampled.add(packageVersionId);
        }
      }
      taskListener.getLogger().println(format("Sampled %d packages", sampled.size()));

      return sampled;
    }
  }

  /**
   * Creates a packageNode for a specific packageVersion, and queues it for dependency resolution
   */
  private void enqueueForBuild(PackageVersionId packageVersionId) throws InterruptedException {

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
      if(resolvedDependency.hasBuild()) {
        node.provideBuild(resolvedDependency.getBuildNumber(), resolvedDependency.getBuildOutcome());
      } else {
        // We will need to build this one as well...
        resolveDependencies(node);
      }
    }
    return node;
  }

  private void resolveDependencies(PackageNode node) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    taskListener.getLogger().println(format("Resolving dependencies of %s...", node.getId()));

    ResolvedDependencySet resolution;
    try {
      resolution = RenjinCiClient.resolveDependencies(node.getId());

    } catch (Exception e) {
      taskListener.getLogger().println(format("Failed to resolve dependencies of %s: %s", node.getId(), e.getMessage()));
      e.printStackTrace(taskListener.getLogger());
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
}
