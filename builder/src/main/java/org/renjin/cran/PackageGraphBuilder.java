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

  /**
   * List of projects that still need to be built
   */
  private List<PackageNode> toBuild;

  /**
   * Set of packages scheduled to build
   */
  private Set<PackageNode> scheduled = Sets.newHashSet();

  /**
   * Set of all projects that have been successfully built.
   */
  private Set<PackageNode> built = Sets.newHashSet();

  private int numConcurrentBuilds = 4;

  public PackageGraphBuilder(Workspace workspace) {
    this.workspace = workspace;
    this.reporter = new BuildReporter(workspace);
  }

  public void addPackage(PackageNode packageNode) {
    nodes.put(packageNode.getName(), packageNode);
  }

  public void build() throws InterruptedException, ExecutionException {

    Map<String, BuildResult> results = Maps.newHashMap();

    toBuild = Lists.newArrayList(nodes.values());
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numConcurrentBuilds);
    service = new ExecutorCompletionService<BuildResult>(executor);

    System.out.println("Thread pool created with " + numConcurrentBuilds + " threads");

    while(true) {

      // schedule any packages whose dependencies have been met
      ListIterator<PackageNode> it = toBuild.listIterator();
      while(it.hasNext()) {
        PackageNode pkg = it.next();
        if(dependenciesAreResolved(pkg)) {
          scheduleForBuild(pkg);

          it.remove();
        }
      }

      // Is our queue empty? In that case any remaining items
      // to build have unresolvable dependencies
      if(scheduled.isEmpty()) {
        break;
      }

      // wait for the next package to complete
      BuildResult result = service.take().get();
      PackageNode completed = nodes.get(result.getPackageName());
      scheduled.remove(completed);

      results.put(result.getPackageName(), result);

      System.out.println(result.getPackageName() + ": " + result.getOutcome());

      // if it's succeeded, add to list of packages that
      // are now available as dependencies
      if(result.getOutcome() == BuildOutcome.SUCCESS) {
        built.add(completed);

      } else if(result.getOutcome() == BuildOutcome.ERROR ||
        result.getOutcome() == BuildOutcome.TIMEOUT) {
        // otherwise reschedule a few times
        // it's possible to encounter OutOfMemory Errors
        Integer retries = retryCount.get(result.getPackageName());
        if(retries == null) {
          retries = 0;
        }
        if(retries < 3) {
          // reschedule
          scheduleForBuild(nodes.get(result.getPackageName()));
          retryCount.put(result.getPackageName(), retries+1);
        }
      }

      // report status periodically
      if(results.size() % 50 == 0) {
        System.out.println(results.size() + "/" + nodes.size() + " builds completed; " + built.size() + " successful.");
      }
    }

    // close down the thread pool so that the process can exit
    executor.shutdown();

    System.out.println("Build complete; " + toBuild.size() + " package(s) with unmet dependencies");

    for(PackageNode node : toBuild) {
      results.put(node.getName(), new BuildResult(node.getName(), BuildOutcome.NOT_BUILT));
    }
  }

  private void scheduleForBuild(PackageNode pkg) {
    System.out.println("Scheduling " + pkg + "...");

    this.service.submit(new PackageBuilder(workspace, reporter, pkg));
    scheduled.add(pkg);
  }

  private boolean dependenciesAreResolved(PackageNode pkg) {
    for(PackageDescription.PackageDependency node : pkg.getDescription().getDepends()) {
      if(!node.getName().equals("R") && !CorePackages.isCorePackage(node.getName())) {
        PackageNode depNode = nodes.get(node.getName());
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
