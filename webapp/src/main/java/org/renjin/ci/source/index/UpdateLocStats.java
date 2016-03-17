package org.renjin.ci.source.index;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.Loc;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;

import java.util.Collection;
import java.util.List;


public class UpdateLocStats {
  
  private static final int BATCH_SIZE = 100;
  
  private Loc cranLoc = new Loc("org.renjin.cran");
  private Loc bioconductorLoc = new Loc("org.renjin.bioconductor");
  private Loc total = new Loc("_TOTAL_");

  public void run() {
    
    // Fetch the list of all Packages, but batch the fetching of LOC entities in groups
    
    Iterable<Package> packages = PackageDatabase.getPackages();
    List<Key<Loc>> packageVersionIds = Lists.newArrayList();
    for (Package aPackage : packages) {
      packageVersionIds.add(Loc.key(aPackage.getLatestVersionId()));
      if(packageVersionIds.size() > BATCH_SIZE) {
        count(packageVersionIds);
        packageVersionIds.clear();
      }
    }
    if(!packageVersionIds.isEmpty()) {
      count(packageVersionIds);
    }
    
    ObjectifyService.ofy().save().entities(cranLoc, bioconductorLoc, total).now();
  }

  private void count(List<Key<Loc>> packageVersionIds) {
    Collection<Loc> counts = ObjectifyService.ofy().load().keys(packageVersionIds).values();
    for (Loc count : counts) {
      String groupId = count.getPackageVersionId().getGroupId();
      total.add(count);
      if(groupId.equals(cranLoc.getGroupId())) {
        cranLoc.add(count);
      } else if(groupId.equals(bioconductorLoc.getGroupId())) {
        bioconductorLoc.add(count);
      }
    }
  }
}
