package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.RenjinVersionStat;
import org.renjin.ci.pipelines.Pipelines;
import org.renjin.ci.stats.ReComputeBuildDeltas;
import org.renjin.ci.stats.StatPipelines;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/qa")
public class QaResources {

  @GET
  @Path("updateBuildDeltas")
  public Response updateBuildDeltas() {
    
    
    
    return Pipelines.redirectToStatus(Pipelines.forEach(new ReComputeBuildDeltas()));  
  }

  @GET
  @Path("updateBuildDeltaCounts")
  public Response updateBuildDeltaCounts() {
    return Pipelines.redirectToStatus(StatPipelines.startUpdateBuildStats());
  }
  
  @GET
  @Path("progress")
  public Viewable getProgress() {

    Map<String, Object> model = new HashMap<>();
    model.put("stats", ObjectifyService.ofy().load().type(RenjinVersionStat.class).list());
    
    return new Viewable("/progress.ftl", model);

  }
  
  @GET
  @Path("progress/{renjinVersion}")
  public Viewable getDeltas(@PathParam("renjinVersion") String renjinVersion) {

    QueryResultIterable<PackageBuild> builds = ObjectifyService.ofy().load()
        .type(PackageBuild.class)
        .filter("buildDelta <", 100)
        .iterable();

    List<PackageBuild> versionBuilds = Lists.newArrayList();
    for (PackageBuild build : builds) {
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
