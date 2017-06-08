package org.renjin.ci.packages;

import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.TreeMultimap;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.RenjinVersionId;

import java.util.*;

public class RenjinBuildGrouper {

  private PackageBuild build;
  private TreeMultimap<RenjinVersionId, PackageBuild> buildMap;
  private List<RenjinBuildHistoryGroup> groups = new ArrayList<>();

  public RenjinBuildGrouper(PackageBuild build, TreeMultimap<RenjinVersionId, PackageBuild> buildMap) {
    this.build = build;
    this.buildMap = buildMap;
  }

  public List<RenjinBuildHistoryGroup> group() {

    // The builds are organized by Renjin Version, but we don't want to show all versions
    // by default because there will be long series of the same results.

    // First, identify the change points and other interesting builds we want to highlight
    NavigableSet<RenjinVersionId> versions = buildMap.keySet();
    SortedSet<RenjinVersionId> displayedVersions = visibleVersions();

    // Now add a group for each visible version, and a group for the hidden versions in between

    PeekingIterator<RenjinVersionId> versionIt = Iterators.peekingIterator(displayedVersions.iterator());
    while(versionIt.hasNext()) {
      RenjinVersionId version = versionIt.next();

      groups.add(new RenjinBuildHistoryGroup(renjinVersionHistory(version)));

      if(versionIt.hasNext()) {
        addHiddenGroup(version, versionIt.peek());
      } else {
        addHiddenGroup(version);
      }
    }

    return groups;
  }

  private void addHiddenGroup(RenjinVersionId from, RenjinVersionId to) {
    addHiddenGroup(buildMap.keySet().subSet(from, false, to, false));
  }

  private void addHiddenGroup(RenjinVersionId from) {
    addHiddenGroup(buildMap.keySet().tailSet(from, false));
  }

  private void addHiddenGroup(NavigableSet<RenjinVersionId> versions) {
    List<RenjinBuildHistory> histories = new ArrayList<>();
    for (RenjinVersionId version : versions) {
      histories.add(renjinVersionHistory(version));
    }
    if(!histories.isEmpty()) {
      groups.add(new RenjinBuildHistoryGroup(histories));
    }
  }

  private RenjinBuildHistory renjinVersionHistory(RenjinVersionId version) {
    return new RenjinBuildHistory(version, buildMap.get(version));
  }

  private SortedSet<RenjinVersionId> visibleVersions() {
    NavigableSet<RenjinVersionId> versions = buildMap.keySet();
    SortedSet<RenjinVersionId> displayedVersions = new TreeSet<>(Ordering.<RenjinVersionId>natural().reverse());

    // Always show the first and latest build
    displayedVersions.add(versions.first());
    displayedVersions.add(versions.last());

    // And show the version for the build we're showing
    displayedVersions.add(build.getRenjinVersionId());

    // Start with the latest version and walk backwards highlighting changes.
    Iterator<RenjinVersionId> versionIt = versions.iterator();
    PackageBuild previousBuild = buildMap.get(versionIt.next()).first();
    while(versionIt.hasNext()) {
      RenjinVersionId version = versionIt.next();
      PackageBuild lastBuildForVersion = buildMap.get(version).first();

      if(change(previousBuild, lastBuildForVersion)) {
        displayedVersions.add(previousBuild.getRenjinVersionId());
        displayedVersions.add(version);
      }
      previousBuild = lastBuildForVersion;
    }
    return displayedVersions;
  }


  private boolean change(PackageBuild a, PackageBuild b) {
    if(a.getOutcome() != b.getOutcome()) {
      return true;
    }
    if(a.getNativeOutcome() != b.getNativeOutcome()) {
      return true;
    }
    return false;
  }
}
