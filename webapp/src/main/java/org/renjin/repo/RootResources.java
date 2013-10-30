package org.renjin.repo;


import com.google.common.collect.Maps;
import com.sun.jersey.api.view.Viewable;
import org.renjin.repo.task.CranTasks;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Map;

@Path("/")
public class RootResources {


  @GET
  @Path("index.html")
  public Viewable getIndex() {

    EntityManager em = HibernateUtil.getActiveEntityManager();
    BuildResultDAO dao = new BuildResultDAO(em);

    Map<String, Object> model = Maps.newHashMap();
    model.put("buildResults", dao.queryResults(13));

    return new Viewable("/index.ftl", model);
  }

  @Path("tasks/cran")
  public CranTasks getTasks() {
    return new CranTasks();
  }

  @Path("builds")
  public BuildResources getBuilds() {
    return new BuildResources();
  }

  @Path("commits")
  public CommitResources getVersions() {
    return new CommitResources();
  }
}
