package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.mapreduce.MapJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapSpecification;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.inputs.DatastoreKeyInput;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pipelines {

  private static final Logger LOGGER = Logger.getLogger(Pipelines.class.getName());

  public static String applyAll(ForEachEntity function) {

    MapJob<Entity, Void, Void> mapJob = newMapJob(function);

    return PipelineServiceFactory.newPipelineService().startNewPipeline(mapJob);
  }
  
  public static <T> MapJob<Entity, Void, Void> newMapJob(ForEachEntity function) {
    MapSpecification<Entity, Void, Void> spec =
        new MapSpecification.Builder<Entity, Void, Void>(
            new DatastoreInput(function.getEntityKind(), 10), function)
            .setJobName(function.getJobName())
            .build();

    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    LOGGER.log(Level.INFO, "Settings: " + settings);

    return new MapJob<>(spec, settings);
  }
  
  public static String forEach(ForEachPackageVersion function) {
    return PipelineServiceFactory.newPipelineService().startNewPipeline(newMapPackageVersionJob(function));
  }
  

  public static <T> MapJob<Key, Void, Void> newMapPackageVersionJob(ForEachPackageVersion function) {
    MapSpecification<Key, Void, Void> spec =
        new MapSpecification.Builder<Key, Void, Void>(
            new DatastoreKeyInput("PackageVersion", 10), function)
            .setJobName(function.getJobName())
            .build();

    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    LOGGER.log(Level.INFO, "Settings: " + settings);

    return new MapJob<>(spec, settings);
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
