package org.renjin.repo.task;


import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

public class AnalysisTasks {


  public Response recalculate() {
    return Response.ok().build();
  }


}
