package org.renjin.repo;

import com.google.appengine.api.datastore.Entity;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.model.RPackageBuildResult;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;


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
