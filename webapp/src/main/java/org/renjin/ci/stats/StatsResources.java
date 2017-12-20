package org.renjin.ci.stats;


import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.glassfish.jersey.server.mvc.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/stats")
public class StatsResources {

  @GET
  public Viewable getIndex() {

    Map<String, Object> model = new HashMap<>();
    model.put("page", new StatsPage());

    return new Viewable("/stats.ftl", model);
  }

  @POST
  @Path("/scheduleCountUpdate")
  public Response scheduleStatsUpdate() {
    StatPipelines.startUpdateBuildStats();

    return Response.ok().build();
  }

  @POST
  @Path("/scheduleGradeTotals")
  public Response scheduleGradeTotals() {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder
        .withUrl("/stats/updateGradeTotals")
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3)));

    return Response.ok().build();
  }


  @POST
  @Path("/updateGradeTotals")
  public Response updateGradeTotals() {

    new GradeTotalsComputer().compute();
    return Response.ok().build();
  }
}
