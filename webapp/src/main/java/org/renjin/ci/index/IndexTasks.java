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
public class IndexTasks {

  private static final Logger LOGGER = Logger.getLogger(IndexTasks.class.getName());

  private final PipelineService pipelineService = PipelineServiceFactory.newPipelineService();

  @Path("cran")
  public CranTasks getCranTasks() {
    return new CranTasks();
  }

  @Path("bioc")
  public BioconductorTasks getBioConductorTasks() {
    return new BioconductorTasks();
  }


}
