package org.renjin.ci.workflow.graph;

import hudson.model.TaskListener;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.ResolvedDependencySet;

import java.util.*;

import static java.lang.String.format;

public class PackageGraphBuilder {
  
  private TaskListener taskListener;

  private final Map<PackageVersionId, PackageNode> nodes = new HashMap<PackageVersionId, PackageNode>();

  public PackageGraphBuilder(TaskListener taskListener) {
    this.taskListener = taskListener;
  }
  
  public PackageGraph add(String filter, Double sample) throws Exception {
    return add(filter, Collections.<String, String>emptyMap(), sample);
  }
  
  public PackageGraph add(String filter, Map<String, String> filterParameters, Double sample) throws Exception {

    if(filter.contains(":")) {
      // consider as packageId
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(filter);
      add(packageVersionId);
    } else {
      addAll(filter, filterParameters, sample);
    }

    taskListener.getLogger().printf("Dependency graph built with %d nodes.\n", nodes.size());

    return new PackageGraph(nodes);
  }

  private void addAll(String filter, Map<String, String> filterParameters, Double sample) throws InterruptedException {
    taskListener.getLogger().println(format("Querying list of '%s' packages...\n", filter));
    List<PackageVersionId> packageVersionIds = RenjinCiClient.queryPackageList(filter, filterParameters);
    taskListener.getLogger().printf("Found %d packages.\n", packageVersionIds.size());

    List<PackageVersionId> sampled = sample(packageVersionIds, sample);

    taskListener.getLogger().println("Building dependency graph...");
    taskListener.getLogger().flush();

    for (PackageVersionId packageVersionId : sampled) {
      add(packageVersionId);

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

  private void add(PackageVersionId packageVersionId) throws InterruptedException {
    PackageNode node = nodes.get(packageVersionId);
    if(node == null) {
      node = new PackageNode(packageVersionId);
      nodes.put(node.getId(), node);

      resolveDependencies(node);
    }
  }

  private PackageNode getOrCreateNodeForDependency(ResolvedDependency resolvedDependency) throws InterruptedException {
    PackageVersionId pvid = resolvedDependency.getPackageVersionId();
    PackageNode node = nodes.get(pvid);
    if(node == null) {
      node = new PackageNode(pvid);
      nodes.put(node.getId(), node);

      // Add dependencies
      if(resolvedDependency.hasBuild()) {
        node.provideBuild(resolvedDependency.getBuildNumber());
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
      node.orphan();
      return;
    }
    if(!resolution.isComplete()) {
      taskListener.getLogger().println(node.getId() + " has unknown dependencies " + resolution.getMissingPackages());
      node.orphan();

    } else {
      for (ResolvedDependency resolvedDependency : resolution.getDependencies()) {
        node.dependsOn(getOrCreateNodeForDependency(resolvedDependency) );
      }
    }
  }
}
