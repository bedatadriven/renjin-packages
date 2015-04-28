package org.renjin.ci.packages.results;


import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.collect.Maps;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.*;
import org.renjin.ci.datastore.Package;

import java.util.*;

public class PackageResults {

  private SortedMap<RenjinVersionId, RenjinResults> renjinMap = Maps.newTreeMap();
  private Map<Long, PackageBuild> buildMap = new HashMap<>();
  private Map<Long, TestRun> testRuns = new HashMap<>();

  public RenjinResults get(RenjinVersionId id) {
    RenjinResults renjin = renjinMap.get(id);
    if(renjin == null) {
      renjin = new RenjinResults(id);
      renjinMap.put(renjin.getId(), renjin);
    }
    return renjin;
  }
  
  public Collection<RenjinResults> getVersions()  {
    return renjinMap.values();
  }

  public void build(PackageId packageId) {

    QueryResultIterable<PackageBuild> builds = ObjectifyService.ofy().load()
        .type(PackageBuild.class)
        .ancestor(Package.key(packageId))
        .iterable();

    QueryResultIterable<PackageTestResult> testResults = ObjectifyService.ofy().load()
        .type(PackageTestResult.class)
        .ancestor(Package.key(packageId))
        .iterable();

    for (PackageBuild build : builds) {
      buildMap.put(build.getBuildNumber(), build);
      get(build.getRenjinVersionId()).add(build);
    }

    Iterator<RenjinVersionId> iterator = renjinMap.keySet().iterator();
    if(iterator.hasNext()) {
      boolean building = renjinMap.get(iterator.next()).isBuilding();

      while (iterator.hasNext()) {
        RenjinResults result = renjinMap.get(iterator.next());
        if(building == result.isBuilding()) {
          result.setBuildDelta(0);
        } else if(building && !result.isBuilding()) {
          result.setBuildDelta(-1);
        } else {
          result.setBuildDelta(+1);
        }
      }
    }
  }
}
