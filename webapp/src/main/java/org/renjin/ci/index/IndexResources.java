package org.renjin.ci.index;

import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * Endpoints triggered by the cron job
 */
@Path("/tasks/index")
public class IndexResources {

  private static final Logger LOGGER = Logger.getLogger(IndexResources.class.getName());

  private final PipelineService pipelineService = PipelineServiceFactory.newPipelineService();

  @GET
  @Path("updateCran")
  public Response updateCran() {
    String jobId = pipelineService.startNewPipeline(new FetchCranUpdates());

    LOGGER.info("Pipeline " + jobId + " started.");

    return Response.ok().build();
  }

  @GET
  @Path("updateGit/{sha}")
  public Response updateGit(@PathParam("sha") String sha) {
    String jobId = pipelineService.startNewPipeline(new IndexCommit(), sha);
    return Pipelines.redirectToStatus(jobId);
  }


}
