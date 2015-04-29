package org.renjin.ci.workflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.workflow.graph.PackageGraph;
import org.renjin.ci.workflow.graph.PackageNode;
import org.renjin.ci.workflow.tools.WebApp;

import javax.inject.Inject;
import javax.sql.rowset.Predicate;
import java.util.*;

import static java.lang.String.format;


public class PackageGraphExecution extends AbstractSynchronousStepExecution<PackageGraph> {
  
  @Inject
  private PackageGraphStep step;

  @StepContextParameter
  private transient TaskListener taskListener;

  private transient Map<PackageVersionId, PackageNode> nodes;
  

  @Override
  protected PackageGraph run() throws Exception {
    
    nodes = Maps.newHashMap();
    
    if("not built".equals(step.getFilter())) {
      addAll("unbuilt");
    } else if(step.getFilter().contains(":")) {
      // consider as packageId
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(step.getFilter());
      add(packageVersionId);
    }
    
    taskListener.getLogger().printf("Dependency graph built with %d nodes.\n", nodes.size());
    
    return new PackageGraph(nodes);
  }

  private void addAll(String filter) throws InterruptedException {
    taskListener.getLogger().println(format("Querying list of '%s' packages...\n", filter));
    List<PackageVersionId> packageVersionIds = WebApp.queryPackageList(filter);
    taskListener.getLogger().printf("Found %d packages.\n", packageVersionIds.size());

    List<PackageVersionId> sampled = sample(packageVersionIds);
    
    taskListener.getLogger().println("Building dependency graph...");
    taskListener.getLogger().flush();
    
    for (PackageVersionId packageVersionId : sampled) {
      add(packageVersionId);
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
    }
  }

  private List<PackageVersionId> sample(List<PackageVersionId> packageVersionIds) {
    if(step.getSample() == null) {
      return packageVersionIds;
    } else {
      double fraction;
      if(step.getSample() > 1) {
        taskListener.getLogger().println(format("Sampling %.0f packages randomly", step.getSample()));
        fraction = step.getSample() / (double)packageVersionIds.size();
      } else {
        fraction = step.getSample();
        taskListener.getLogger().println(format("Sampling %7.6f of packages randomly", step.getSample()));
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

  private void add(PackageVersionId packageVersionId) {
    PackageNode node = nodes.get(packageVersionId);
    if(node == null) {
      node = new PackageNode(packageVersionId);
      nodes.put(node.getId(), node);
      
      resolveDependencies(node);
    }
  }

  private PackageNode getOrCreateNodeForDependency(ResolvedDependency resolvedDependency) {
    PackageVersionId pvid = resolvedDependency.getPackageVersionId();
    PackageNode node = nodes.get(pvid);
    if(node == null) {
      node = new PackageNode(pvid);
      nodes.put(node.getId(), node);

      // Add dependencies
      if(resolvedDependency.hasBuild()) {
        resolvedDependency.setBuildNumber(resolvedDependency.getBuildNumber());
      } else {
        // We will need to build this one as well...
        resolveDependencies(node);
      }
    }
    return node;
  }

  private void resolveDependencies(PackageNode node) {
    taskListener.getLogger().println(format("Resolving dependencies of %s...", node.getId()));
    
    for (ResolvedDependency resolvedDependency : WebApp.resolveDependencies(node.getId())) {
      node.dependsOn(getOrCreateNodeForDependency(resolvedDependency) );
    }

    node.setDependenciesResolved(true);
  }
}
