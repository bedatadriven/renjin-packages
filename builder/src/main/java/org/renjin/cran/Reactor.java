package org.renjin.cran;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.renjin.repo.model.*;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Reactor {

  private Workspace workspace;

  private BuildReporter reporter;

  private ExecutorCompletionService<BuildResult> service;

  private Map<String, PackageNode> nodes = Maps.newHashMap();

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

  public Reactor(Workspace workspace, Map<String, PackageNode> nodes) {
    this.workspace = workspace;
    this.reporter = new BuildReporter(workspace);
    this.nodes = nodes;
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
          scheduleForBuild(pkg);

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

  private void scheduleForBuild(PackageNode pkg) {

    // check if we've already succeeded in building this package node
    if(reporter.packageAlreadySucceeded(pkg.getId())) {
      System.out.println(pkg + " already successfully built for this commit");
      built.add(pkg);
    } else {
      System.out.println("Scheduling " + pkg + "...");

      this.service.submit(new PackageBuilder(workspace, reporter, pkg));
      scheduled.add(pkg);
    }
  }

  private boolean dependenciesAreResolved(PackageNode pkg) {
    for(PackageEdge dep : pkg.getEdges()) {
      if(!built.contains(dep.getDependency())) {
        return false;
      }
    }
    return true;
  }
}
