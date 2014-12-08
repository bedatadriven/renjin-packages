package org.renjin.ci.index;

import com.google.appengine.tools.pipeline.PipelineServiceFactory;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;


@Path("/webhooks")
public class WebHooks {

  private static final Logger LOGGER = Logger.getLogger(WebHooks.class.getName());

  @POST
  public Response github(@HeaderParam("X-Github-Event") String event, @QueryParam("head") String commitSha) {

    LOGGER.info("Event type: " + event);
    LOGGER.info("Head: " + commitSha);

    String jobId = PipelineServiceFactory.newPipelineService().startNewPipeline(new IndexCommit(), commitSha);

    LOGGER.info("Started job: " + jobId);

    return Response.ok().build();
  }
}
