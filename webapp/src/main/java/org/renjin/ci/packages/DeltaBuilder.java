package org.renjin.ci.packages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.*;
import java.util.logging.Logger;

import static org.renjin.ci.datastore.PackageBuild.*;


/**
 * Examine the results of builds of this package version against sequential versions of Renjin and identify
 * builds that represent "regressions" and "progressions".
 *
 * A regression is evidence that a change in Renjin has led to a failure in package building. A progression
 * is evidence that a change in Renjin has fixed an existing defect in the Renjin build process.
 *
 * For example, if survey:2.5 fails to build against
 * Renjin 0.7.1514, but successfully builds against Renjin 0.7.1534, then this is an improvement
 * we want to highlight. 
 *
 * <p>However, if we try to build again with Renjin 0.7.1560 and it fails, then that PackageBuild is
 * a regression to flag immediately.</p>
 *
 */
public class DeltaBuilder {

  private static final Logger LOGGER = Logger.getLogger(DeltaBuilder.class.getName());
  
  private final PackageVersionId packageVersionId;
  private final Map<Long, BuildDelta> deltas = new HashMap<>();

  public DeltaBuilder(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  private BuildDelta delta(PackageBuild build) {
    BuildDelta delta = deltas.get(build.getBuildNumber());
    if(delta == null) {
      delta = new BuildDelta(build);
      deltas.put(build.getBuildNumber(), delta);
    }
    return delta;
  }

  private BuildDelta delta(PackageTestResult result) {
    BuildDelta delta = deltas.get(result.getPackageBuildNumber());
    if(delta == null) {
      delta = new BuildDelta(result.getPackageBuildNumber(), result.getRenjinVersionId());
      deltas.put(result.getPackageBuildNumber(), delta);
    }
    return delta;
  }


  public PackageVersionDelta build(Optional<PackageBuild> newBuild, List<PackageTestResult> newTestResults) {

    List<PackageBuild> builds = PackageDatabase.getFinishedBuilds(packageVersionId);

    if (!builds.isEmpty()) {

      // Build a simplified list, mapping each renjin version in order to a build
      // If there have been multiple builds for a given Renjin Version, use the last build
      TreeMap<RenjinVersionId, PackageBuild> buildMap = latestBuildPerRenjinVersion(builds, newBuild);

      // Query the test results for these builds that we've included in the analysis
      Iterable<PackageTestResult> testResults = Iterables.concat(
          PackageDatabase.getTestResults(buildMap.values()),
          newTestResults);

      checkBuildHistory(buildMap);
      checkCompilationHistory(buildMap);
      checkTestResults(testResults);
      
      // Annotate the build deltas with last good build
      for (BuildDelta delta : deltas.values()) {
        PackageBuild previousSuccessfulBuild = findPreviousSuccessfulBuild(delta.getRenjinVersionId(), buildMap);
        if(previousSuccessfulBuild != null) {
          delta.setLastSuccessfulBuild(previousSuccessfulBuild.getBuildNumber());
          delta.setLastSuccessfulRenjinVersion(previousSuccessfulBuild.getRenjinVersion());
        }
      }
    }
    return new PackageVersionDelta(packageVersionId, deltas.values());
  }

  private PackageBuild findPreviousSuccessfulBuild(RenjinVersionId renjinVersionId, TreeMap<RenjinVersionId, PackageBuild> buildMap) {
    Map.Entry<RenjinVersionId, PackageBuild> previous = buildMap.lowerEntry(renjinVersionId);
    if(previous == null) {
      return null;
    }
    while(!previous.getValue().isSucceeded()) {
      previous = buildMap.lowerEntry(previous.getKey());
      if(previous == null) {
        return null;
      }
    }
    return previous.getValue();
  }

  private TreeMap<RenjinVersionId, PackageBuild> latestBuildPerRenjinVersion(List<PackageBuild> builds, Optional<PackageBuild> newBuild) {

    TreeMap<RenjinVersionId, PackageBuild> buildMap = Maps.newTreeMap();
    for (PackageBuild build : builds) {
      RenjinVersionId rv = build.getRenjinVersionId();
      if(isValidRenjinVersion(rv)) {
        PackageBuild lastBuild = buildMap.get(rv);

        if (lastBuild == null || build.getBuildNumber() > lastBuild.getBuildNumber()) {
          buildMap.put(rv, build);
        }
      }
    }
    
    // If we have just updated a new build within the same transaction, it may not yet be
    // returned by getFinishedBuilds(), or may be out of date. 
    // Ensure that we use the updated version here
    if(newBuild.isPresent()) {
      buildMap.put(newBuild.get().getRenjinVersionId(), newBuild.get());
    }
    
    return buildMap;
  }

  private void checkCompilationHistory(TreeMap<RenjinVersionId, PackageBuild> buildMap) {
    // Consider only those builds where we have native compilation results and look for 
    // progressions/regressions among those.
    Iterable<PackageBuild> nativeCompilation = Iterables.filter(buildMap.values(), nativeCompilationAttempted());

    Optional<PackageBuild> nativeRegression = findRegression(nativeCompilation, nativeCompilationSucceeded());
    if(nativeRegression.isPresent()) {
      delta(nativeRegression.get()).setCompilationDelta(-1);
    }
    Optional<PackageBuild> nativeProgression = findProgression(nativeCompilation, nativeCompilationSucceeded());
    if(nativeProgression.isPresent()) {
      delta(nativeProgression.get()).setCompilationDelta(+1);
    }
  }

  private void checkBuildHistory(TreeMap<RenjinVersionId, PackageBuild> buildMap) {
    Optional<PackageBuild> buildRegression = findRegression(buildMap.values(), buildSucceeded());
    if(buildRegression.isPresent()) {
      delta(buildRegression.get()).setBuildDelta(-1);
    }
    Optional<PackageBuild> buildProgression = findProgression(buildMap.values(), buildSucceeded());
    if(buildProgression.isPresent()) {
      delta(buildProgression.get()).setBuildDelta(+1);
    }
  }


  private void checkTestResults(Iterable<PackageTestResult> testResults) {
    
    // Now do the same for tests, one test at a time
    Multimap<String, PackageTestResult> tests = indexByName(testResults);
    for (String testName : tests.keySet()) {

      Collection<PackageTestResult> results = tests.get(testName);
      
      // Identify regression or progression among the build selected for comparison
      TreeMap<RenjinVersionId, PackageTestResult> byVersion = resultsFromLatestBuilds(results);

      Optional<PackageTestResult> regression = findRegression(byVersion.values(), testSucceeded());
      if(regression.isPresent()) {
        delta(regression.get()).getTestRegressions().add(regression.get().getName());
      }
      Optional<PackageTestResult> progression = findProgression(byVersion.values(), testSucceeded());
      if(progression.isPresent()) {
        delta(progression.get()).getTestProgressions().add(progression.get().getName());
      }
    }
  }


  /**
   * Take a collection of all results for a particular test, and order those from the selected LATEST
   * builds by Renjin version id.
   */
  private static TreeMap<RenjinVersionId, PackageTestResult> resultsFromLatestBuilds(
      Collection<PackageTestResult> results) {
    
    TreeMap<RenjinVersionId, PackageTestResult> treeMap = new TreeMap<>();
    for (PackageTestResult result : results) {
        treeMap.put(result.getRenjinVersionId(), result);
    }
    return treeMap;
  }

  private static Predicate<PackageTestResult> testSucceeded() {
    return new Predicate<PackageTestResult>() {
      @Override
      public boolean apply(PackageTestResult input) {
        return input.isPassed();
      }
    };
  }

  private static Multimap<String, PackageTestResult> indexByName(Iterable<PackageTestResult> testResults) {
    Multimap<String, PackageTestResult> map = HashMultimap.create();
    for (PackageTestResult testResult : testResults) {
      map.put(testResult.getName(), testResult);
    }
    return map;
  }

  private static Collection<TreeMap<RenjinVersionId, PackageTestResult>> indexTests(
      Iterable<PackageTestResult> testResults) {
    
    
    Map<String, TreeMap<RenjinVersionId, PackageTestResult>> map = new HashMap<>();
    for (PackageTestResult result : testResults) {

      // Get this test's renjin => result map
      TreeMap<RenjinVersionId, PackageTestResult> versionMap = map.get(result.getName());
      if(versionMap == null) {
        versionMap = new TreeMap<>();
        map.put(result.getName(), versionMap);
      }

      // Add this result to the map keyed by the renjin version with which it was run
      versionMap.put(result.getRenjinVersionId(), result);
    }
    return map.values();
  }

  @VisibleForTesting
  static <T> Optional<T> findRegression(Iterable<T> builds, Predicate<T> predicate) {

    // Order the builds from the build against the most recent version of Renjin to the build
    // against the oldest version of Renjin
    if(!Iterables.isEmpty(builds)) {
      List<T> history = Lists.newArrayList(builds);
      Collections.reverse(history);

      ListIterator<T> it = history.listIterator();

      // Is the build currently failing? Than this is a regression that needs to be fixed.

      T mostRecent = it.next();
      if (!predicate.apply(mostRecent)) {

        T previousBuild = mostRecent;
        while (it.hasNext()) {

          // Given a sequence that looks like this:
          //   Present  ----------------------> PAST
          // [ 0:FAILED, 1:FAILED, 2:SUCCESS, 3:SUCCESS, 4:FAILED, 5:FAILED, 6: SUCCESS]

          // We want to tag the build at index 1 as a regression, because it is the first to
          // break the sequence of successes. Build #5 could also be considered a regression,
          // but we're ignoring it for this purpose because it's no longer actionable: we only
          // want to tag essentially UNRESOLVED regressions that need to be fixed.

          T build = it.next();
          if (predicate.apply(build)) {
            return Optional.of(previousBuild);
          }
          previousBuild = build;
        }
      }
    }

    return Optional.absent();
  }

  @VisibleForTesting
  static <T> Optional<T> findProgression(Iterable<T> builds, Predicate<T> predicate) {

    // IF the package was initially failing, then the first build to succeed is considered a progression,
    // no matter what happens after that.

    Iterator<T> it = builds.iterator();

    if(it.hasNext()) {
      T firstBuild = it.next();
      if (!predicate.apply(firstBuild)) {
        
        // First build is failing, see if there's a subsequent success.
        while (it.hasNext()) {
          T build = it.next();
          if (predicate.apply(build)) {
            return Optional.of(build);
          }
        }
      }
    }
    return Optional.absent();
  }

  private static boolean isValidRenjinVersion(RenjinVersionId rv) {
    if(rv.toString().contains("SNAPSHOT")) {
      return false;
    }
    if(rv.toString().equals("LATEST")) {
      return false;
    }
    if(rv.toString().contains("RC")) {
      // Discount these very old builds, sometimes the package "built"
      // because we weren't really do everything we should have been doing
      return false;
    }
    return true;
  }


  public static void update(PackageVersionId packageVersionId, Optional<PackageBuild> newBuild, List<PackageTestResult> testResults) {
    DeltaBuilder builder = new DeltaBuilder(packageVersionId);
    PackageVersionDelta deltas = builder.build(newBuild, testResults);

    ObjectifyService.ofy().save().entity(deltas).now();
  }
}
