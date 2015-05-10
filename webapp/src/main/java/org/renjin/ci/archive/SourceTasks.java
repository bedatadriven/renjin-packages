package org.renjin.ci.archive;

import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/tasks/source")
public class SourceTasks {

  @GET
  @Path("/examples/start")
  public Response extractExamples() {

    return Pipelines.redirectToStatus(Pipelines.forEach(new ExamplesExtractor()));
    
  }

}
