package org.renjin.build.worker;


import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.renjin.build.task.PackageBuildResult;
import org.renjin.build.task.PackageBuildTask;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static Logger LOGGER = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) throws IOException, InterruptedException {
    File workspace = new File("/tmp/workspace/packages");

    while(true) {
      Optional<PackageBuildTask> task = leaseNextBuild();
      if(!task.isPresent()) {
        LOGGER.info("No tasks available, sleeping 60s...");
        Thread.sleep(60_000);
      } else {

        try {
          // wrap up last task:
          PackageBuilder builder = new PackageBuilder(workspace, task.get());
          builder.build();

          LOGGER.log(Level.INFO, task.get().packageBuildId() + ": " +
              builder.getOutcome());

          // report back on outcome
          reportResult(builder.getResult());

        } catch(Exception e) {
          LOGGER.log(Level.SEVERE, "Exception while building " + task.get().getPackageName(), e);
        }
      }
    }
  }


  public static Optional<PackageBuildTask> leaseNextBuild() {

    try {
      Client client = ClientBuilder.newClient().register(JacksonFeature.class);
      WebTarget target = client.target("http://localhost:8080").path("queue").path("lease");

      Form form = new Form();
      form.param("workerId", getWorkerId());

      PackageBuildTask task =
          target.request(MediaType.APPLICATION_JSON_TYPE)
              .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
                  PackageBuildTask.class);


      return Optional.fromNullable(task);

    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while leasing build...", e);
      return Optional.absent();
    }
  }

  private static void reportResult(PackageBuildResult result) {
    try {
      Client client = ClientBuilder.newClient().register(JacksonFeature.class);
      WebTarget target = client.target("http://localhost:8080").path("queue").path("result");

      int status = target.request()
          .post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE))
          .getStatus();

      LOGGER.log(Level.INFO, "Reported " + result.getId() + ": status = " + status);

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while reporting result of build", e);
    }
  }

  private static String getWorkerId() {
    String id = System.getenv("HOSTNAME");
    if(Strings.isNullOrEmpty(id)) {
      id = "worker" + Long.toString(Thread.currentThread().getId());
    }
    return id;
  }
}
