package org.renjin.build.model;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.googlecode.objectify.ObjectifyService.*;

/**
 * Provides basic query operations on
 * database of Packages
 */
public class PackageDatabase {

  static {
    register(PackageBuild.class);
    register(org.renjin.build.model.Package.class);
    register(PackageVersion.class);
    register(PackageStatus.class);

    JodaTimeTranslators.add(factory());
    factory().getTranslators().add(new IdTranslatorFactory<>(PackageVersionId.class));
    factory().getTranslators().add(new IdTranslatorFactory<>(PackageBuild.class));
    factory().getTranslators().add(new IdTranslatorFactory<>(RenjinVersionId.class));

  }

  public static Optional<PackageVersion> getPackageVersion(PackageVersionId id) {
    return Optional.fromNullable(ofy().load().key(Key.create(PackageVersion.class, id.toString())).now());
  }


  public static Optional<PackageVersion> getPackageVersion(String packageVersionId) {
    return getPackageVersion(PackageVersionId.fromTriplet(packageVersionId));
  }

  public static List<PackageVersion> queryPackageVersions(String groupId, String packageName) {

    // PackageVersions are keyed by groupId:packageName:versionXXX so we can use
    // lexical graphical ordering properties to query by key

    QueryResultIterator<PackageVersion> iterator = ofy().load()
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

  public static void save(PackageVersion packageVersion) {
    ofy().save().entities(packageVersion);
  }

  public static PackageStatus getStatus(PackageVersionId packageVersionId, RenjinVersionId renjinVersionId) {
    PackageStatus status = ofy().load().key(PackageStatus.key(packageVersionId, renjinVersionId)).now();
    if(status == null) {
      status = new PackageStatus(packageVersionId, renjinVersionId);
      status.setBuildStatus(BuildStatus.BLOCKED);
    }
    return status;
  }

  public static Collection<PackageStatus> getStatus(Set<PackageVersionId> packageVersionIds, RenjinVersionId release) {
    List<Key<PackageStatus>> keys = Lists.newArrayList();
    for(PackageVersionId id : packageVersionIds) {
      keys.add(Key.create(PackageStatus.class, id.toString()));
    }

    return ofy().load().keys(keys).values();
  }

  public static void save(PackageStatus status) {
    ofy().save().entity(status);
  }
}
