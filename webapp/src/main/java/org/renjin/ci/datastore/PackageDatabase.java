package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.googlecode.objectify.*;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Query;
import org.renjin.ci.model.*;
import org.renjin.ci.repo.apt.AptArtifact;
import org.renjin.ci.repo.apt.AptDist;
import org.renjin.ci.repo.apt.AptPackage;
import org.renjin.ci.repo.apt.PgpKey;
import org.renjin.sexp.ExternalPtr;
import org.renjin.sexp.ListVector;
import org.renjin.sexp.NamedValue;
import org.renjin.util.DataFrameBuilder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.register;

/**
 * Provides basic query operations on
 * database of Packages
 */
public class PackageDatabase {
  
  private static final Logger LOGGER = Logger.getLogger(PackageDatabase.class.getName());

  public static Random RANDOM = new Random();

  static {
    init();
  }

  public static void init() {
    register(PackageBuild.class);
    register(Package.class);
    register(PackageVersion.class);
    register(PackageVersionDescription.class);
    register(PackageVersionDelta.class);
    register(PackageTestResult.class);
    register(TestRegression.class);

    register(PackageExample.class);
    register(PackageExampleSource.class);
    register(PackageExampleRun.class);
    register(ReplacementRelease.class);
    register(RenjinVersionStats.class);
    register(RenjinVersionTotals.class);
    register(RenjinCommit.class);
    register(RenjinRelease.class);
    register(LastEventTime.class);
    
    register(BenchmarkMachine.class);
    register(BenchmarkRun.class);
    register(BenchmarkResult.class);
    register(BenchmarkNumber.class);
    register(BenchmarkSummary.class);
    
    register(PackageSource.class);
    register(FunctionIndex.class);
    
    register(Loc.class);

    register(Artifact.class);

    register(SystemRequirement.class);

    register(AptArtifact.class);
    register(AptDist.class);
    register(AptPackage.class);


    register(PgpKey.class);
  }

  public static Objectify ofy() {
    return ObjectifyService.ofy();
  }

  public static Optional<PackageVersion> getPackageVersion(PackageVersionId id) {
    return Optional.fromNullable(ObjectifyService.ofy().load().key(PackageVersion.key(id)).now());
  }

  public static Optional<PackageVersion> getPackageVersion(String packageVersionId) {
    return getPackageVersion(PackageVersionId.fromTriplet(packageVersionId));
  }
  
  public static QueryResultIterable<PackageVersion> getLatestPackageReleases() {

    return ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .order("-publicationDate")
        .limit(20);
  }

  public static List<Package> getPackagesStartingWithLetter(char letter) {

    Iterable<Package> lowerCase = getPackagesStartingWithCharacter(Character.toLowerCase(letter));
    Iterable<Package> upperCase = getPackagesStartingWithCharacter(Character.toUpperCase(letter));
    
    List<Package> packages = Lists.newArrayList();
    Iterables.addAll(packages, lowerCase);
    Iterables.addAll(packages, upperCase);

    Collections.sort(packages, Ordering.natural().onResultOf(new Function<Package, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(Package input) {
        return input.getName().toLowerCase();
      }
    }));
    
    return packages;
  }


  public static QueryResultIterable<Package> getPackages() {
    return ObjectifyService.ofy().load()
        .type(Package.class)
        .chunk(1000)
        .iterable();
  }


  public static QueryResultIterable<Package> getPackagesStartingWithCharacter(char letter) {
    String lowerKey = Character.toString(letter);
    String upperKey = Character.toString((char) (letter + 1));

    LOGGER.info("Querying between " + lowerKey + " and " + upperKey);


    return ObjectifyService.ofy().load()
        .type(Package.class)
        .filter("name >=", lowerKey)
        .filter("name <", upperKey)
        .chunk(1000)
        .iterable();
  }


  public static List<PackageVersion> queryPackageVersions(Package entity) {
    return getPackageVersions(entity.getPackageId());
  }


  public static QueryResultIterable<Key<PackageVersion>> getPackageVersionIds() {

    return ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .chunk(1000)
        .keys()
        .iterable();

  }

  public static List<PackageVersion> getPackageVersions(PackageId packageId) {

    // PackageVersions are keyed by groupId:packageName:versionXXX so we can use
    // lexical graphical ordering properties to query by key

    return ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .ancestor(Package.key(packageId))
        .list();

  }


  public static List<PackageVersion> getPackageVersionsByName(String packageName) {

    // PackageVersions are keyed by groupId:packageName:versionXXX so we can use
    // lexical graphical ordering properties to query by key

    return ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .filter("packageName", packageName)
        .list();

  }
  
  public static Iterable<PackageVersionId> getPackageVersionIds(PackageId packageId) {
    QueryResultIterable<Key<PackageVersion>> versions = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .ancestor(Package.key(packageId))
        .keys()
        .iterable();
    
    return Iterables.transform(versions, new Function<Key<PackageVersion>, PackageVersionId>() {
      @Override
      public PackageVersionId apply(Key<PackageVersion> key) {
        return PackageVersion.idOf(key);
      }
    });
  }

  public static Query<PackageBuild> getBuilds(PackageId packageId) {

    return ObjectifyService.ofy().load()
            .type(PackageBuild.class)
            .ancestor(Package.key(packageId));
  }

  public static Query<PackageBuild> getBuilds(PackageVersionId packageVersionId) {

    return ObjectifyService.ofy().load()
        .type(PackageBuild.class)
        .ancestor(PackageVersion.key(packageVersionId));
  }

  public static List<PackageBuild> getFinishedBuilds(PackageVersionId packageVersionId) {
    
    QueryResultIterable<PackageBuild> list = ObjectifyService.ofy().load()
        .type(PackageBuild.class)
        .ancestor(PackageVersion.key(packageVersionId))
        .iterable();
    
    List<PackageBuild> finished = new ArrayList<>();
    for (PackageBuild build : list) {
      if(build.isFinished()) {
        finished.add(build);
      }
    }
    
    return finished;
  }


  public static LoadResult<PackageBuild> getBuild(PackageVersionId packageVersionId, long buildNumber) {
    return ObjectifyService.ofy().load().key(PackageBuild.key(packageVersionId, buildNumber));
  }

  public static Result<Map<Key<PackageVersion>,PackageVersion>> save(PackageVersion... packageVersions) {
    return ObjectifyService.ofy().save().entities(packageVersions);
  }

  public static Result<Map<Key<Object>, Object>> save(List<Object> objects) {
    return ObjectifyService.ofy().save().entities(objects);
  }


  public static Result<Map<Key<Object>, Object>> save(Object... objects) {
    return ObjectifyService.ofy().save().entities(objects);
  }

  public static void saveNow(Object entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }
  

  public static Optional<RenjinCommit> getCommit(String commitHash) {
    Key<RenjinCommit> key = Key.create(RenjinCommit.class, commitHash);
    return Optional.fromNullable(ObjectifyService.ofy().load().key(key).now());
  }

  public static Iterable<RenjinRelease> getReleases() {
    return ObjectifyService.ofy().load().type(RenjinRelease.class).iterable();
  }

  public static Optional<Package> getPackageIfExists(PackageVersionId packageVersionId) {
    return getPackageIfExists(packageVersionId.getPackageId());
  }

  public static Optional<Package> getPackageIfExists(PackageId packageId) {
    return Optional.fromNullable(getPackage(packageId).now());
  }

  public static LoadResult<Package> getPackage(PackageId packageId) {
    return ObjectifyService.ofy()
        .load()
        .key(Key.create(Package.class, packageId.toString()));
  }

  public static Package getPackageOf(PackageVersionId packageVersionId) {
    Package pkg = ObjectifyService.ofy()
        .load()
        .key(Key.create(Package.class, packageVersionId.getGroupId() + ":" + packageVersionId.getPackageName()))
        .now();
    
    if(pkg == null) {
      throw new IllegalStateException("Package entity does not exist: " + packageVersionId.getPackageId());
    }
    
    return pkg;
  }

  public static QueryResultIterable<PackageTestResult> getTestResults(PackageBuildId packageBuildId) {

    return ObjectifyService.ofy()
        .load()
        .type(PackageTestResult.class)
        .ancestor(PackageBuild.key(packageBuildId))
        .iterable();
  }

  public static Iterable<PackageTestResult> getTestResults(Iterable<PackageBuild> builds) {
    List<Iterable<PackageTestResult>> tests = Lists.newArrayList();
    for (PackageBuild build : builds) {
      tests.add(getTestResults(build.getId()));
    }
    return Iterables.concat(tests);
  }

  public static QueryResultIterable<PackageTestResult> getTestResults(PackageVersionId packageVersionId) {
    
    return ObjectifyService.ofy()
            .load()
            .type(PackageTestResult.class)
            .ancestor(PackageVersion.key(packageVersionId))
            .chunk(200)
            .iterable();
  }

  public static Query<PackageTestResult> getTestResults(PackageId packageId) {

    return ObjectifyService.ofy()
        .load()
        .type(PackageTestResult.class)
        .ancestor(Package.key(packageId));

  }


  public static List<PackageVersion> getPackageVersions(String groupId, String name) {
    return getPackageVersions(new PackageId(groupId, name));
  }

  public static Query<TestRegression> getOpenTestRegressions() {
    return getTestRegressions()
        .filter("open", true);
  }

  public static LoadType<TestRegression> getTestRegressions() {
    return ObjectifyService.ofy()
        .load()
        .type(TestRegression.class);
  }

  public static LoadResult<Key<TestRegression>> getNextOpenTestRegression(Key<TestRegression> regressionKey) {
    return getTestRegressions()
        .orderKey(false)
        .filterKey(">", regressionKey)
        .keys()
        .first();
  }

  public static Query<TestRegression> getTestRegressions(PackageVersionId packageVersionId) {
    return getTestRegressions()
        .ancestor(PackageVersionDelta.key(packageVersionId));
  }

  public static LoadResult<TestRegression> getTestRegression(PackageBuildId packageBuildId, String testName) {
    return ObjectifyService.ofy()
        .load()
        .key(TestRegression.key(packageBuildId, testName));
  }

  public static Loader load() {
    return ObjectifyService.ofy().load();
  }


  public static RenjinVersionStats getRenjinVersionStats() {
    return ObjectifyService.ofy().load().key(RenjinVersionStats.singletonKey()).now();
  }
  
  public static Iterable<PackageTestResult> getTestResults(PackageVersionId packageVersionId, final String testName) {
    Iterable<PackageTestResult> testResults = ObjectifyService.ofy().load()
        .type(PackageTestResult.class)
        .ancestor(PackageVersion.key(packageVersionId))
        .filter("name", testName)
        .iterable();
    
    return testResults;
  }

  public static Iterable<PackageTestResult> getTestResults(PackageId packageId, final String testName) {
    Iterable<PackageTestResult> testResults = ObjectifyService.ofy().load()
        .type(PackageTestResult.class)
        .ancestor(Package.key(packageId))
        .filter("name", testName)
        .iterable();

    return testResults;
  }
  
  public static RenjinVersionId getLatestRelease() {
    QueryResultIterator<Key<RenjinRelease>> results = ObjectifyService.ofy()
        .load()
        .type(RenjinRelease.class)
        .order("-buildNumber")
        .limit(1)
        .keys()
        .iterator();
    
    if(!results.hasNext()) {
      throw new IllegalStateException("No releases");
    }
    return RenjinVersionId.valueOf(results.next().getName());
  }


  public static LoadResult<PackageBuild> getBuild(PackageBuildId id) {
    return getBuild(id.getPackageVersionId(), id.getBuildNumber());
  }

  public static Iterable<Key<PackageSource>> getPackageSourceKeys(PackageVersionId packageVersionId) {
    return ObjectifyService.ofy()
        .load()
        .type(PackageSource.class)
        .ancestor(PackageVersion.key(packageVersionId))
        .keys()
        .iterable();
  }

  public static LoadResult<PackageSource> getPackageSource(PackageVersionId packageVersionId, String filename) {
    return ObjectifyService.ofy()
        .load()
        .key(PackageSource.key(packageVersionId, filename));
  }
  
  public static LoadResult<PackageSource> getSource(PackageVersionId packageVersionId, String filename) {
    Key<PackageVersion> parentKey = PackageVersion.key(packageVersionId);
    return ObjectifyService.ofy()
        .load()
        .key(Key.create(parentKey, PackageSource.class, filename));
  }
  
  public static QueryResultIterator<Key<FunctionIndex>> getFunctionUses(String functionName) {
    return ObjectifyService.ofy()
        .load()
        .type(FunctionIndex.class)
        .filter("use", functionName)
        .keys()
        .iterator();
  }

  public static LoadResult<RenjinRelease> getRenjinRelease(RenjinVersionId renjinVersion) {
    return ObjectifyService.ofy()
        .load()
        .key(Key.create(RenjinRelease.class, renjinVersion.toString()));
  }

  public static LoadResult<PackageVersionDelta> getDelta(PackageVersionId packageVersionId) {
    return ObjectifyService.ofy()
        .load()
        .key(Key.create(PackageVersionDelta.class, packageVersionId.toString()));
  }

  public static Set<String> getPackageVersionTags(PackageId packageId) {
    List<PackageVersion> versions = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .ancestor(Package.key(packageId))
        .list();
    
    Set<String> tagNames = Sets.newHashSet();

    for (PackageVersion version : versions) {
      if(version.getTagName() != null) {
        tagNames.add(version.getTagName());
      }
    }
    
    return tagNames;
  }

  public static LoadResult<BenchmarkRun> getBenchmarkRun(long runNumber) {
    return ObjectifyService.ofy().load().key(Key.create(BenchmarkRun.class, runNumber));
  }

  public static Query<BenchmarkMachine> getMostRecentBenchmarkMachines() {
    return ObjectifyService.ofy().load().type(BenchmarkMachine.class).order("-lastUpdated");
  }

  public static LoadResult<BenchmarkMachine> getBenchmarkMachine(String machineId) {
    return ObjectifyService.ofy().load().key(Key.create(BenchmarkMachine.class, machineId));
  }

  public static Query<BenchmarkResult> getBenchmarkResultsForMachine(String machineId) {
    return ObjectifyService.ofy().load().type(BenchmarkResult.class).filter("machineId", machineId);
  }

  public static Query<BenchmarkResult> getBenchmarkResultsForMachine(String machineId, String benchmarkName) {
    return ObjectifyService.ofy().load().type(BenchmarkResult.class)
        .filter("machineId", machineId)
        .filter("benchmarkName", benchmarkName);
  }

  public static LoadResult<Loc> getLinesOfCode(PackageVersionId packageVersionId) {
    return ObjectifyService.ofy().load().key(Loc.key(packageVersionId));
  }

  public static QueryResultIterable<RenjinRelease> getRenjinVersions() {
    return ObjectifyService.ofy()
        .load()
        .type(RenjinRelease.class)
        .chunk(500)
        .iterable();
  }


  public static Query<RenjinVersionTotals> getRenjinVersionTotals() {
    return ObjectifyService.ofy()
        .load()
        .type(RenjinVersionTotals.class)
        .chunk(50);
  }

  public static QueryResultIterable<BenchmarkSummary> getBenchmarkSummaries(String machineId) {
    return ObjectifyService.ofy()
        .load()
        .type(BenchmarkSummary.class)
        .ancestor(Key.create(BenchmarkMachine.class, machineId))
        .iterable();
  }

  public static LoadResult<BenchmarkSummary> getBenchmarkSummary(String machineId, String benchmarkId) {
    return ObjectifyService.ofy().load().key(BenchmarkSummary.key(machineId, benchmarkId));
  }

  public static ListVector query(ExternalPtr entityClassPtr, ListVector filters) {
    Class<?> entityClass = (Class<?>) entityClassPtr.getInstance();
    Query<?> query = ObjectifyService.ofy()
        .load()
        .type(entityClass);

    for (NamedValue namedValue : filters.namedValues()) {
      query = query.filter(namedValue.getName(), namedValue.getValue().asString());
    }

    List list = query.list();
    ListVector dataFrame = DataFrameBuilder.build(entityClass, list);
    
    return dataFrame;
  }

  public static LoadResult<PackageTestResult> getTestResult(PackageBuildId buildId, String testName) {
    return ofy().load().key(PackageTestResult.key(buildId, testName));
  }
}
