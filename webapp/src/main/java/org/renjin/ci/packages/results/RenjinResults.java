package org.renjin.ci.packages.results;

import com.google.common.collect.Sets;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.RenjinVersionId;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Gathers all the results for a given version of Renjin together
 */
public class RenjinResults {
  private RenjinVersionId id;
  private List<TestRun> testRuns = new ArrayList<>();
  private TreeSet<PackageBuild> builds;
  private TestRun lastTestRun = null;
  private int buildDelta;

  public RenjinResults(RenjinVersionId id) {
    this.id = id;
    this.builds = Sets.newTreeSet(PackageBuild.orderByNumber());
  }
  
  public RenjinVersionId getId() {
    return id;
  }

  public void add(PackageBuild build) {
    builds.add(build);
  }

  public SortedSet<PackageBuild> getBuilds() {
    return builds;
  }
  
  public PackageBuild getLastBuild() {
    return builds.last();
  }
  
  public boolean isBuilding() {
    return getLastBuild().isSucceeded();
  }

  public int getBuildDelta() {
    return buildDelta;
  }

  public void setBuildDelta(int buildDelta) {
    this.buildDelta = buildDelta;
  }
  
  public String getBuildDeltaLabel() {
    if(buildDelta < 0) {
      return "-1";
    } else if(buildDelta > 0) {
      return "+1";
    } else {
      return "";
    }
  }
}
