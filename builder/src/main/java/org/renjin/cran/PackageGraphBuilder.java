package org.renjin.cran;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.PackageDescription;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class PackageGraphBuilder {

  private Workspace workspace;

  private BuildReporter reporter;

  private Map<String, Integer> retryCount = Maps.newHashMap();

  private ExecutorCompletionService<BuildResult> service;

  private Map<String, PackageNode> nodes = Maps.newHashMap();
  private Map<String, PackageNode> nodeBySimpleName = Maps.newHashMap();

  /**
   * Set of packages scheduled to build
   */
  private Set<PackageNode> scheduled = Sets.newHashSet();

  /**
   * Set of all projects that have been successfully built.
   */
  private Set<PackageNode> built = Sets.newHashSet();

  private int numConcurrentBuilds = 4;
  private int builtCount = 0;

  public PackageGraphBuilder(Workspace workspace) {
    this.workspace = workspace;
    this.reporter = new BuildReporter(workspace);
  }

  public void addPackage(PackageNode packageNode) {
    nodes.put(packageNode.getPackageVersionId(), packageNode);
    nodeBySimpleName.put(packageNode.getName(), packageNode);
  }

  public void setNumConcurrentBuilds(int numConcurrentBuilds) {
    this.numConcurrentBuilds = numConcurrentBuilds;
  }

  public void build() throws InterruptedException, ExecutionException {

    List<PackageNode> toBuild = Lists.newArrayList(nodes.values());
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numConcurrentBuilds);
    service = new ExecutorCompletionService<BuildResult>(executor);

    System.out.println("Thread pool created with " + numConcurrentBuilds + " threads");

    while(true) {

      // schedule any packages whose dependencies have been met
      ListIterator<PackageNode> it = toBuild.listIterator();
      while(it.hasNext()) {
        PackageNode pkg = it.next();
        if(dependenciesAreResolved(pkg)) {
          scheduleForBuild(pkg, 0);

          it.remove();
        }
      }

      System.out.println("Scheduled queue length: " + scheduled.size());

      // Is our queue empty? In that case any remaining items
      // to build have unresolvable dependencies
      if(scheduled.isEmpty()) {
        break;
      }

      // wait for the next package to complete
      BuildResult result = service.take().get();
      PackageNode completed = nodes.get(result.getPackageVersionId());
      scheduled.remove(completed);

      builtCount ++;

      System.out.println(result.getPackageVersionId() + ": " + result.getOutcome());

      // if it's succeeded, add to list of packages that
      // are now available as dependencies
      if(result.getOutcome() == BuildOutcome.SUCCESS) {
        built.add(completed);

      } else if(result.getOutcome() == BuildOutcome.ERROR ||
        result.getOutcome() == BuildOutcome.TIMEOUT) {
        // otherwise reschedule a few times
        // it's possible to encounter OutOfMemory Errors
        Integer attemptCount = retryCount.get(result.getPackageVersionId());
        if(attemptCount == null) {
          attemptCount = 1;
        }
        if(attemptCount < 3) {
          // reschedule
          PackageNode node = nodes.get(result.getPackageVersionId());
          if(node == null) {
            System.out.println("SEVERE: node lookup on " + result.getPackageVersionId() + " failed");
          } else {
            scheduleForBuild(node, attemptCount+1);
            retryCount.put(result.getPackageVersionId(), attemptCount+1);
          }
        }
      }

      // report status periodically
      if(builtCount % 50 == 0) {
        System.out.println(builtCount + "/" + nodes.size() + " builds completed; " + built.size() + " successful.");
      }
    }

    // close down the thread pool so that the process can exit
    executor.shutdown();

    System.out.println("Build complete; " + toBuild.size() + " package(s) with unmet dependencies");
  }

  private void scheduleForBuild(PackageNode pkg, int previousAttempts) {

    // check if we've already succeeded in building this package node
    if(reporter.packageAlreadySucceeded(pkg.getPackageVersionId())) {
      System.out.println(pkg + " already successfully built for this commit");
      built.add(pkg);
    } else {
      System.out.println("Scheduling " + pkg + "... [previous attempts: " + previousAttempts + "]");

      this.service.submit(new PackageBuilder(workspace, reporter, pkg, previousAttempts));
      scheduled.add(pkg);
    }
  }

  private boolean dependenciesAreResolved(PackageNode pkg) {
    for(PackageDescription.PackageDependency node : pkg.getDescription().getDepends()) {
      if(!node.getName().equals("R") && !CorePackages.isCorePackage(node.getName())) {
        PackageNode depNode = nodeBySimpleName.get(node.getName());
        if(depNode == null) {
          return false;
        }
        if(!built.contains(depNode)) {
          return false;
        }
      }
    }
    return true;
  }
}
