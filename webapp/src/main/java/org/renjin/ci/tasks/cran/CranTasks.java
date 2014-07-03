package org.renjin.ci.tasks.cran;


import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.io.ByteStreams;
import org.glassfish.jersey.server.mvc.Viewable;
import org.joda.time.LocalDate;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.tasks.RegisterPackageVersionTask;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Fetch the latest list of package versions from a CRAN mirror
 */
@Path("/tasks/cran")
public class CranTasks {

  private final static Logger LOGGER = Logger.getLogger(CranTasks.class.getName());


  private final GcsService gcsService =
          GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());


  @GET
  @Path("admin")
  public Viewable getAdminPage() {
    return new Viewable("/cranAdmin.ftl", new HashMap());
  }

  @GET
  @Path("updateIndex")
  public Response updateIndex() {

    // fetch any packages in last week
    LocalDate threshold = new LocalDate().minusDays(7);

    LOGGER.info("Fetching packages updated since: " + threshold);

    List<String> updatedPackageNames = CRAN.fetchUpdatedPackageList(threshold);
    for(String updatedPackage : updatedPackageNames) {
      LOGGER.info("Package " + updatedPackage + " was updated");
      enqueueFetch(updatedPackage);
    }

    return Response.ok().build();
  }


  private void enqueueFetch(String packageName) {
    Queue queue = QueueFactory.getQueue("cran-fetch");
    queue.add(TaskOptions.Builder.withUrl("/tasks/cran/fetch")
            .param("packageName", packageName));
  }

  @POST
  @Path("fetch")
  public void fetchPackage(@FormParam("packageName") String packageName) throws IOException {

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

      RegisterPackageVersionTask.queue(packageVersionId);
    }
  }

  private GcsFilename archiveSourceToGcs(String packageName, String version) throws IOException {
    URL url = CRAN.sourceUrl(packageName, version);
    LOGGER.info("Fetching source from " + url);

    HttpURLConnection urlConnection = getUrl(url);
    int responseCode = urlConnection.getResponseCode();

    if(responseCode == 200) {
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else if(responseCode == 404) {
      LOGGER.warning("Source URL not found, trying archive...");
      URL archiveUrl = CRAN.archivedSourceUrl(packageName, version);
      urlConnection = getUrl(archiveUrl);
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

    GcsOutputChannel outputChannel =
            gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());

    try(
        OutputStream outputStream = Channels.newOutputStream(outputChannel);
        InputStream inputStream = urlConnection.getInputStream()) {

      ByteStreams.copy(inputStream, outputStream);

    }
    return filename;
  }

  private HttpURLConnection getUrl(URL url) throws IOException {
    HttpURLConnection urlConnection =  (HttpURLConnection) url.openConnection ();
    urlConnection.setRequestMethod("GET");
    urlConnection.connect() ;
    return urlConnection;
  }
}
