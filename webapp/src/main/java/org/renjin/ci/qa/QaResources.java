package org.renjin.ci.qa;

import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.qa.compare.ComparisonResource;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/qa")
public class QaResources {

  @Path("/compare/{from}/{to}")
  public ComparisonResource compareReleases(@PathParam("from") RenjinVersionId from, @PathParam("to") RenjinVersionId to) {
    return new ComparisonResource(from, to);
  }

}
