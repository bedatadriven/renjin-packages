package org.renjin.build;

import com.sun.jersey.api.view.Viewable;
import org.renjin.build.model.RPackageBuild;

import javax.ws.rs.GET;


public class ResultResource {
  private final RPackageBuild entity;

  public ResultResource(RPackageBuild result) {
    this.entity = result;
  }

  @GET
  public Viewable getIndex() {
    return new Viewable("/buildResult.ftl", entity);
  }


}
