package org.renjin.ci.worker;


import com.google.common.base.Optional;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.renjin.ci.task.PackageBuildResult;
import org.renjin.ci.task.PackageBuildTask;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static Logger LOGGER = Logger.getLogger(Main.class.getName()).getParent();

  private String instanceId;

  public static void main(String[] args) {

    new Main().run();
  }

  private void run() {
    LOGGER.log(Level.INFO, "Renjin CI Worker Starting...");

    instanceId = getInstanceId();


    LOGGER.log(Level.INFO, "Instance ID: " + instanceId);

    while(true) {

      LOGGER.log(Level.INFO, "Fetching next task...");

      Optional<PackageBuildTask> task = leaseNextBuild();
      if(!task.isPresent()) {
        LOGGER.info("No tasks available, sleeping 60s.");
        try {
          Thread.sleep(60_000);
        } catch (InterruptedException e) {
          return;
        }
      } else {
        try {
          LOGGER.log(Level.INFO, "Building " + task.get().toString());

          PackageBuilder builder = new PackageBuilder(task.get());
          builder.build();

          LOGGER.log(Level.INFO, task.get() + ": " + builder.getOutcome());
          LOGGER.log(Level.INFO, task.get().url());

          // report back on outcome
          reportResult(task.get(), builder.getResult());

        } catch(Exception e) {
          LOGGER.log(Level.SEVERE, "Exception while building " + task.get(), e);
        }
      }
    }
  }

  private String getInstanceId() {
    Client client = ClientBuilder.newClient().register(JacksonFeature.class);
    return client.target("http://metadata.google.internal/computeMetadata/v1/instance/id")
        .request()
        .header("Metadata-Flavor", "Google")
        .get(String.class);
  }

  public Optional<PackageBuildTask> leaseNextBuild() {

    try {
      Client client = ClientBuilder.newClient().register(JacksonFeature.class);
      WebTarget target = client.target("https://renjinci.appspot.com/build/queue/next");

      Form form = new Form();
      form.param("worker", instanceId);

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

  private void reportResult(PackageBuildTask task, PackageBuildResult result) {
    try {
      Client client = ClientBuilder.newClient().register(JacksonFeature.class);
      WebTarget target = client.target("https://renjinci.appspot.com")
          .path("build")
          .path("result")
          .path(task.getPackageVersionId().replace(':', '/'))
          .path(Long.toString(task.getBuildNumber()));

      int status = target.request()
          .post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE))
          .getStatus();

      LOGGER.log(Level.INFO, "Reported " + result.getId() + ": status = " + status);

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while reporting result of build", e);
    }
  }
}
