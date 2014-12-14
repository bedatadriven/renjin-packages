package org.renjin.ci.releases;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.PackageDatabase;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.HashMap;
import java.util.Map;

@Path("/releases")
public class Releases {

  @GET
  @Produces("text/html")
  public Viewable releases() {

    Map<String, Object> model = new HashMap<>();
    model.put("releases", Lists.newArrayList(Iterables.limit(PackageDatabase.getReleases(), 10)));

    return new Viewable("/releases.ftl", model);
  }

}
