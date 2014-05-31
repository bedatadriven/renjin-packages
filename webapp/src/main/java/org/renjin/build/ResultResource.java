package org.renjin.build;

import com.sun.jersey.api.view.Viewable;
import org.renjin.build.model.RPackageBuildResult;

import javax.ws.rs.GET;


public class ResultResource {
  private final RPackageBuildResult entity;

  public ResultResource(RPackageBuildResult result) {
    this.entity = result;
  }

  @GET
  public Viewable getIndex() {
    return new Viewable("/buildResult.ftl", entity);
  }


}
