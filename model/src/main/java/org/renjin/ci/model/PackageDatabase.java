package org.renjin.ci.model;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.Work;

import java.util.*;

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
    register(PackageStatus.class);
    register(PackageTest.class);
    register(VersionComparison.class);
    register(VersionComparisonReport.class);
    register(VersionComparisonEntry.class);
    register(RenjinCommit.class);
    register(RenjinRelease.class);
    register(BuildJob.class);
  }

  public static Optional<PackageVersion> getPackageVersion(PackageVersionId id) {
    return Optional.fromNullable(ObjectifyService.ofy().load().key(Key.create(PackageVersion.class, id.toString())).now());
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

  public static List<PackageVersion> queryPackageVersions(String groupId, String packageName) {

    // PackageVersions are keyed by groupId:packageName:versionXXX so we can use
    // lexical graphical ordering properties to query by key

    QueryResultIterator<PackageVersion> iterator = ObjectifyService.ofy().load()
        .type(PackageVersion.class)
        .filterKey(">=", Key.create(PackageVersion.class, groupId + ":" + packageName))
        .iterator();

    List<PackageVersion> matching = Lists.newArrayList();
    while(iterator.hasNext()) {
      PackageVersion pv = iterator.next();
      PackageVersionId pvId = PackageVersionId.fromTriplet(pv.getId());
      if(pvId.getGroupId().equals(groupId) && pvId.getPackageName().equals(packageName)) {
        matching.add(pv);
      } else {
        // end of the sequence
        break;
      }
    }

    return matching;
  }

  public static List<PackageBuild> getBuilds(PackageVersionId packageVersionId) {

    // Keys are in the form: groupId:packageName:version:buildNumber

    Key<PackageBuild> startKey = Key.create(PackageBuild.class, packageVersionId.toString() + ":");
    Key<PackageBuild> endKey = Key.create(PackageBuild.class, packageVersionId.toString() + "Z");

    return ObjectifyService.ofy().load().type(PackageBuild.class)
        .filterKey(">=", startKey).filterKey("<", endKey).list();
  }

  public static List<PackageStatus> getStatus(String groupId, String packageName) {

    // Keys are in the form: groupId:packageName:version:buildNumber

    String prefix = groupId + ":" + packageName;

    Key<PackageStatus> startKey = Key.create(PackageStatus.class, prefix + ":");
    Key<PackageStatus> endKey = Key.create(PackageStatus.class, prefix + "Z");


    return ObjectifyService.ofy().load().type(PackageStatus.class)
        .filterKey(">=", startKey).filterKey("<", endKey).list();
  }


  public static PackageBuild getBuild(PackageVersionId packageVersionId, long buildNumber) {
    return ObjectifyService.ofy().load().key(PackageBuild.key(packageVersionId, buildNumber)).safe();
  }

  public static Result<Map<Key<PackageVersion>,PackageVersion>> save(PackageVersion... packageVersions) {
    return ObjectifyService.ofy().save().entities(packageVersions);
  }

  public static PackageStatus getStatus(PackageVersionId packageVersionId, RenjinVersionId renjinVersionId) {
    PackageStatus status = ObjectifyService.ofy().load().key(PackageStatus.key(packageVersionId, renjinVersionId)).now();
    if(status == null) {
      status = new PackageStatus(packageVersionId, renjinVersionId);
      status.setBuildStatus(BuildStatus.BLOCKED);
    }
    return status;
  }

  public static Collection<PackageStatus> getStatus(Set<PackageVersionId> packageVersionIds,
                                                    RenjinVersionId renjinVersion) {
    List<Key<PackageStatus>> keys = Lists.newArrayList();
    for(PackageVersionId id : packageVersionIds) {
      keys.add(Key.create(PackageStatus.class, id + ":" + renjinVersion));
    }

    return ObjectifyService.ofy().load().keys(keys).values();
  }

  public static List<PackageStatus> getAllStatusForPackageVersion(PackageVersionId packageVersionId) {

    // PackageStatus entities are ordered by PackageVersionId, so we can do a single range
    // query on key to find status records for a given PV for all versions of renjin
    Key<PackageStatus> beginKey = Key.create(PackageStatus.class, packageVersionId.toString() + ":");

    QueryResultIterator<PackageStatus> it = ObjectifyService.ofy()
        .load()
        .type(PackageStatus.class)
        .filterKey(">=", beginKey)
        .iterator();

    List<PackageStatus> results = Lists.newArrayList();
    while(it.hasNext()) {
      PackageStatus status = it.next();
      if(!status.getPackageVersionId().equals(packageVersionId)) {
        break;
      }
      results.add(status);
    }

    return results;
  }

  public static Optional<Key<PackageStatus>> getNextReady() {

    QueryResultIterable<Key<PackageStatus>> keys = ObjectifyService.ofy()
        .load()
        .type(PackageStatus.class)
        .filter("buildStatus = ", BuildStatus.READY.name())
        .keys()
        .iterable();

    // Choose a key at random to avoid contention between multiple workers
    List<Key<PackageStatus>> top = Lists.newArrayList(Iterables.limit(keys, 40));
    if(top.isEmpty()) {
      return Optional.absent();

    } else {
      int index = RANDOM.nextInt(top.size());
      return Optional.of(top.get(index));
    }
  }

  public static void save(PackageStatus status) {
    ObjectifyService.ofy().save().entity(status).now();
  }

  public static VersionComparison getVersionComparison(RenjinVersionId from, RenjinVersionId to) {
    Key<VersionComparison> key = VersionComparison.key(from, to);
    VersionComparison comparison = ObjectifyService.ofy().load().key(key).now();
    if(comparison == null) {
      comparison = new VersionComparison(from, to);
    }
    return comparison;
  }

  public static Optional<RenjinCommit> getCommit(String commitHash) {
    Key<RenjinCommit> key = Key.create(RenjinCommit.class, commitHash);
    return Optional.fromNullable(ObjectifyService.ofy().load().key(key).now());
  }

  public static Iterable<RenjinRelease> getReleases() {
    return ObjectifyService.ofy().load().type(RenjinRelease.class).iterable();
  }
}
