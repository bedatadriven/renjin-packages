package org.renjin.ci;


import org.renjin.ci.migrate.MigrateBuilds;
import org.renjin.ci.model.TestResult;
import org.renjin.ci.tasks.cran.CranTasks;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class RootResources {



  @Path("tasks/cran")
  public CranTasks getTasks() {
    return new CranTasks();
  }


  @Path("migrateBuilds")
  public MigrateBuilds migrate() {
    return new MigrateBuilds();
  }

  @GET
  @Path("tests/results/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getTestResult(@PathParam("id") int id) {
    return HibernateUtil.getActiveEntityManager().find(TestResult.class, id).getOutput();
  }
}
