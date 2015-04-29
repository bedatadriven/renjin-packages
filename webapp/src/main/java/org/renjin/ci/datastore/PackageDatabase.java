package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.googlecode.objectify.ObjectifyService.register;

/**
 * Provides basic query operations on
 * database of Packages
 */
public class PackageDatabase {

  public static Random RANDOM = new Random();

  static {
    init();
  }

  public static void init() {
    register(PackageBuild.class);
    register(Package.class);
    register(PackageVersion.class);
    register(PackageTestResult.class);
    register(PackageTestRun.class);
    register(RenjinVersionStat.class);
    register(RenjinCommit.class);
    register(RenjinRelease.class);
    register(LastEventTime.class);
    register(Benchmark.class);
    register(BenchmarkEnvironment.class);
    register(BenchmarkResult.class);
  }

  public static Optional<PackageVersion> getPackageVersion(PackageVersionId id) {
    return Optional.fromNullable(ObjectifyService.ofy().load().key(PackageVersion.key(id)).now());
  }

  public static Optional<PackageVersion> getPackageVersion(String packageVersionId) {
    return getPackageVersion(PackageVersionId.fromTriplet(packageVersionId));
  }

  public static long newBuildNumber(final PackageVersionId packageVersionId) {
    return ObjectifyService.ofy().transact(new Work<Long>() {

      @Override
      public Long run() {
        PackageVersion pv = ObjectifyService.ofy().load().key(Key.create(PackageVersion.class, packageVersionId.toString())).safe();
        long number = pv.getLastBuildNumber();
        if(number == 0) {
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

  public static List<PackageVersion> getPackageVersions(PackageId packageId) {

    // PackageVersions are keyed by groupId:packageName:versionXXX so we can use
    // lexical graphical ordering properties to query by key

    return ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .ancestor(Package.key(packageId))
        .list();

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


  public static PackageBuild getBuild(PackageVersionId packageVersionId, long buildNumber) {
    return ObjectifyService.ofy().load().key(PackageBuild.key(packageVersionId, buildNumber)).safe();
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
  
  
}
