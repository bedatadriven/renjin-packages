package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.collect.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.PackageBuild;
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

    QueryResultIterable<PackageBuild> buildDelta = ofy().load()
        .type(PackageBuild.class)
        .filter("buildDelta <", 100)
        .iterable();

    QueryResultIterable<PackageBuild> compilationDelta = ofy().load()
        .type(PackageBuild.class)
        .filter("compilationDelta <", 100)
        .iterable();

    List<PackageBuild> versionBuilds = Lists.newArrayList();
    for (PackageBuild build : Iterables.concat(buildDelta, compilationDelta)) {
      if(build.getRenjinVersion().equals(renjinVersion)) {
        versionBuilds.add(build);
      }
    }
    
    Map<String, Object> model = new HashMap<>();
    model.put("renjinVersion", renjinVersion);
    model.put("builds", versionBuilds);
    

    return new Viewable("/versionDeltas.ftl", model);
  }
}
