package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.MapJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapSpecification;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pipelines {

  private static final Logger LOGGER = Logger.getLogger(Pipelines.class.getName());

  public static <T> String applyAll(EntityMapFunction<T> function) {

    MapSpecification<Entity, Void, Void> spec =
        new MapSpecification.Builder<Entity, Void, Void>(
            new DatastoreInput(function.getEntityName(), 10), function)
            .setJobName(function.getJobName())
            .build();

    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    LOGGER.log(Level.INFO, "Settings: " + settings);

    MapJob<Entity, Void, Void> mapJob = new MapJob<>(spec, settings);

    return PipelineServiceFactory.newPipelineService().startNewPipeline(mapJob);
  }

  public static Response redirectToStatus(String pipelineId) {
    String destinationUrl =  "/_ah/pipeline/status.html?root=" + pipelineId;
    try {
      return Response.seeOther(new URI(destinationUrl)).build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
//
//  private void redirectToPipelineStatus(HttpServletResponse resp,
//                                        String pipelineId) throws IOException {
//    String destinationUrl = getPipelineStatusUrl(pipelineId);
//    log.info("Redirecting to " + destinationUrl);
//    resp.sendRedirect(destinationUrl);
//  }
//
//  private String getPipelineStatusUrl(String pipelineId) {
//    return "/_ah/pipeline/status.html?root=" + pipelineId;
//  }

}
