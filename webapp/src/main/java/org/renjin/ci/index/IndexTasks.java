package org.renjin.ci.index;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.cloudstorage.*;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import com.google.common.io.ByteStreams;
import org.joda.time.LocalDate;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.List;
import java.util.logging.Logger;

/**
 * Endpoints triggered by the cron job
 */
@Path("/tasks/index")
public class IndexTasks {

  private static final Logger LOGGER = Logger.getLogger(IndexTasks.class.getName());

  private final PipelineService pipelineService = PipelineServiceFactory.newPipelineService();

  @GET
  @Path("updateCran")
  public Response updateCran() {

    Queue queue = QueueFactory.getQueue("cran-fetch");
    TaskHandle taskHandle = queue.add(TaskOptions.Builder.withUrl("/tasks/index/fetchCranUpdates"));

    LOGGER.info("Enqueued task to check for updated CRAN packages: " + taskHandle.getName());

    return Response.ok().build();
  }
  
  @POST
  @Path("fetchCranUpdates")
  public Response fetchCranUpdates() {

    LocalDate threshold = new LocalDate().minusYears(2);

    LOGGER.info("Fetching packages updated since: " + threshold);

    Queue queue = QueueFactory.getQueue("cran-fetch");
    List<String> recentlyUpdatedPackages = CRAN.fetchUpdatedPackageList(threshold);
    
    LOGGER.info("Found " + recentlyUpdatedPackages.size() + " recently updated packages.");
    
    for(String updatedPackage : recentlyUpdatedPackages) {
      queue.add(TaskOptions.Builder
              .withUrl("/tasks/index/fetchCranPackage")
              .param("packageName", updatedPackage));
    }
    
    return Response.ok().build();
  }
  
  @POST
  @Path("fetchCranPackage")
  public Response fetchCranPackage(@FormParam("packageName") String packageName) throws IOException {

    LOGGER.info("Fetching latest version of " + packageName + " from CRAN");

    // get the latest version from CRAN
    String version = CRAN.fetchLatestPackageVersion(packageName);
    PackageVersionId packageVersionId = new PackageVersionId("org.renjin.cran", packageName, version);

    LOGGER.info("Latest version is " + packageVersionId);

    // Do we have this already?
    if(PackageDatabase.getPackageVersion(packageVersionId).isPresent()) {

      LOGGER.info("Already present in database");

    } else {

      LOGGER.info("New version: Fetching source...");

      // archive the source to
      archiveSourceToGcs(packageName, version);

      // Register the package version in our database
      Queue queue = QueueFactory.getDefaultQueue();
      queue.add(TaskOptions.Builder.withUrl("/tasks/register").param("packageVersionId", packageVersionId.toString()));
      
    }
    return Response.ok().build();
  }


  private GcsFilename archiveSourceToGcs(String packageName, String version) throws IOException {
    URL url = CRAN.sourceUrl(packageName, version);
    LOGGER.info("Fetching source from " + url);

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int responseCode = urlConnection.getResponseCode();

    if(responseCode == 200) {
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else if(responseCode == 404) {
      LOGGER.warning("Source URL not found, trying archive...");
      URL archiveUrl = CRAN.archivedSourceUrl(packageName, version);
      urlConnection = (HttpURLConnection) archiveUrl.openConnection();
      responseCode = urlConnection.getResponseCode();
      if(responseCode != 200) {
        LOGGER.severe("Error fetching " + archiveUrl + ": " + responseCode);
      }
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else {
      LOGGER.severe("Error fetching source: " + responseCode);
      throw new WebApplicationException(Response.Status.ACCEPTED);
    }
  }

  private GcsFilename archiveSourceToGcs(HttpURLConnection urlConnection, String packageName, String version) throws IOException {

    GcsFilename filename = CRAN.gcsFileName(packageName, version);
    LOGGER.info("Storing source archive at " + filename);


    GcsService gcsService =
            GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    GcsOutputChannel outputChannel =
            gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());

    try(
            OutputStream outputStream = Channels.newOutputStream(outputChannel);
            InputStream inputStream = urlConnection.getInputStream()) {

      ByteStreams.copy(inputStream, outputStream);

    }
    return filename;
  }


  @GET
  @Path("updateGit/{sha}")
  public Response updateGit(@PathParam("sha") String sha) {
    String jobId = pipelineService.startNewPipeline(new IndexCommit(), sha);
    return Pipelines.redirectToStatus(jobId);
  }


}
