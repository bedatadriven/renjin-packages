package org.renjin.build.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.renjin.build.PomBuilder;
import org.renjin.build.archive.GcsAppIdentityServiceUrlSigner;
import org.renjin.build.jenkins.JenkinsClient;
import org.renjin.build.jenkins.Job;
import org.renjin.build.model.*;
import org.renjin.build.storage.StorageKeys;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;


@Path("/tasks/queuePackageBuilds")
public class QueuePackageBuildTask {

  private static final Logger LOGGER = Logger.getLogger(QueuePackageBuildTask.class.getName());
  private GcsAppIdentityServiceUrlSigner signer = new GcsAppIdentityServiceUrlSigner();


  @GET
  public Response pollQueue() throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyManagementException {

    JenkinsClient jenkinsClient = new JenkinsClient();
    String jobs = jenkinsClient.getJobs();

    LOGGER.log(Level.INFO, "Jenkins Queue: " + jobs);

    return Response.ok().build();
  }

  @POST
  @Path("test")
  public Response test() throws Exception {
    Optional<PackageStatus> next = PackageDatabase.getNextReady();
    if(next.isPresent()) {
      createNewBuild(next.get());
    }
    return Response.ok().build();
  }


  private void createNewBuild(PackageStatus status) throws Exception {

    // get the next build number
    long buildNumber = PackageDatabase.newBuildNumber(status.getPackageVersionId());

    // create the new build entity
    PackageBuild build = new PackageBuild(status.getPackageVersionId(), buildNumber);
    build.setRenjinVersion(status.getRenjinVersionId().toString());
    build.setDependencies(status.getDependencies());
    ofy().save().entity(build);

    // load the description file and generate the pom for this build
    PackageVersion packageVersion = ofy().load().key(status.getPackageVersionId().key()).safe();
    PomBuilder pomBuilder = new PomBuilder(build, packageVersion.parseDescription());
    launchJob(packageVersion.getPackageVersionId(), pomBuilder);

  }

  @VisibleForTesting
  public void launchJob(PackageVersionId packageVersionId, PomBuilder pomBuilder) throws Exception {
    // tell jenkins to go!
    Job job = new Job();
    job.addParameter("groupId", packageVersionId.getGroupId());
    job.addParameter("packageName", packageVersionId.getPackageName());
    job.addParameter("sourceVersion", packageVersionId.getSourceVersion());
    job.addFileParameter("pom.xml", pomBuilder.getXml(), MediaType.APPLICATION_XML);

    JenkinsClient jenkinsClient = new JenkinsClient();
    jenkinsClient.start(job);
  }

  private String getSignedSourceUrl(PackageVersionId versionId) throws Exception {
    return signer.getSignedUrl("GET", StorageKeys.BUCKET_NAME,
        StorageKeys.packageSource(versionId.getGroupId(), versionId.getPackageName(), versionId.getSourceVersion()));
  }

}
