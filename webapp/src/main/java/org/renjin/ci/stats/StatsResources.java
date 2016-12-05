package org.renjin.ci.stats;


import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/stats")
public class StatsResources {
  
  @POST
  @Path("/scheduleCountUpdate")
  public Response scheduleStatsUpdate() {
    StatPipelines.startUpdateBuildStats();

    return Response.ok().build();
  }
}
