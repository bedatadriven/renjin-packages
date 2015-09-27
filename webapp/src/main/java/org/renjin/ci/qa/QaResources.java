package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.datastore.RenjinVersionStat;
import org.renjin.ci.model.RenjinVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.*;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/qa")
public class QaResources {
  

  
  @GET
  @Path("dashboard")
  public Viewable getDashboard() {

    Multimap<RenjinVersionId, RenjinVersionStat> statMap = HashMultimap.create();
    for (RenjinVersionStat stat : ofy().load().type(RenjinVersionStat.class)) {
      statMap.put(stat.getRenjinVersionId(), stat);
    }
    
    List<RenjinVersionSummary> versions = new ArrayList<>();
    for (RenjinVersionId renjinVersionId : statMap.keySet()) {
      versions.add(new RenjinVersionSummary(renjinVersionId, statMap.get(renjinVersionId)));
    }

    Collections.sort(versions, Ordering.natural().reverse());
    
    Map<String, Object> model = new HashMap<>();    
    model.put("versions", versions);
    
    return new Viewable("/dashboard.ftl", model);
  }
  
  @GET
  @Path("progress/{renjinVersion}")
  public Viewable getDeltas(@PathParam("renjinVersion") String renjinVersion) {
    
    QueryResultIterable<PackageVersionDelta> deltas = ofy().load()
        .type(PackageVersionDelta.class)
        .filter("renjinVersions", renjinVersion)
        .iterable();

    List<DeltaViewModel> packages = new ArrayList<>();
    for (PackageVersionDelta delta : deltas) {
      Optional<BuildDelta> build = delta.getBuild(renjinVersion);
      if(build.isPresent()) {
        packages.add(new DeltaViewModel(delta.getPackageVersionId(), build.get()));
      }
    }
    
    Map<String, Object> model = new HashMap<>();
    model.put("renjinVersion", renjinVersion);
    model.put("packageVersions", packages);
    

    return new Viewable("/versionDeltas.ftl", model);
  }
}
