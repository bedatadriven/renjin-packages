package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.*;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Query;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

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
    register(PackageTestResult.class);
    register(TestOutput.class);

    register(PackageExample.class);
    register(PackageExampleSource.class);
    register(PackageExampleRun.class);
    register(RenjinVersionStat.class);
    register(RenjinCommit.class);
    register(RenjinRelease.class);
    register(LastEventTime.class);
    register(Benchmark.class);
    register(BenchmarkEnvironment.class);
    register(BenchmarkResult.class);
    
    register(PackageSource.class);
    register(FunctionIndex.class);
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

  public static QueryResultIterable<Package> getPackagesStartingWithCharacter(char letter) {
    Key<Package> lowerKey = Key.create(Package.class, "org.renjin.cran:" + letter);
    Key<Package> upperKey = Key.create(Package.class, "org.renjin.cran:" + ((char) (letter + 1)));

    LOGGER.info("Querying between " + lowerKey + " and " + upperKey);


    return ObjectifyService.ofy().load()
        .type(Package.class)
        .filterKey(">=", lowerKey)
        .filterKey("<", upperKey)
        .chunk(1000)
        .iterable();
  }

  public static long newBuildNumber(final PackageVersionId packageVersionId) {
    return ObjectifyService.ofy().transact(new Work<Long>() {

      @Override
      public Long run() {
        PackageVersion pv = ObjectifyService.ofy().load().key(Key.create(PackageVersion.class, packageVersionId.toString())).safe();
        long number = pv.getLastBuildNumber();
        if (number == 0) {
          number = 200;
        }
        number++;
        pv.setLastBuildNumber(number);
        ObjectifyService.ofy().save().entity(pv).now();
        return number;
      }
    });
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

  public static Optional<RenjinCommit> getCommit(String commitHash) {
    Key<RenjinCommit> key = Key.create(RenjinCommit.class, commitHash);
    return Optional.fromNullable(ObjectifyService.ofy().load().key(key).now());
  }

  public static Iterable<RenjinRelease> getReleases() {
    return ObjectifyService.ofy().load().type(RenjinRelease.class).iterable();
  }

  public static Optional<Package> getPackageOf(PackageVersionId packageVersionId) {
    return Optional.fromNullable(ObjectifyService.ofy().load().key(Key.create(Package.class,
        packageVersionId.getGroupId() + ":" + packageVersionId.getPackageName())).now());
    
  }

  public static QueryResultIterable<PackageTestResult> getTestResults(PackageBuildId packageBuildId) {

    return ObjectifyService.ofy()
        .load()
        .type(PackageTestResult.class)
        .ancestor(PackageBuild.key(packageBuildId))
        .iterable();
  }

  public static QueryResultIterable<PackageTestResult> getTestResults(PackageVersionId packageVersionId) {
    
    return ObjectifyService.ofy()
            .load()
            .type(PackageTestResult.class)
            .ancestor(PackageVersion.key(packageVersionId))
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


  public static Loader load() {
    return ObjectifyService.ofy().load();
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

}
