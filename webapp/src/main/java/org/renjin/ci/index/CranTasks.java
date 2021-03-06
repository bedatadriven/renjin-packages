package org.renjin.ci.index;

import com.google.appengine.api.taskqueue.*;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.joda.time.LocalDate;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.source.index.SourceIndexTasks;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


public class CranTasks {

  public static final String CRAN_FETCH_QUEUE = "cran-fetch";

  private static final Logger LOGGER = Logger.getLogger(CranTasks.class.getName());

  @GET
  @Path("enqueue")
  public Response updateCran(@HeaderParam("X-Appengine-Cron") String cron) {

    if(!"true".equals(cron)) {
      throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
    }

    Queue queue = QueueFactory.getQueue(CRAN_FETCH_QUEUE);
    TaskHandle taskHandle = queue.add(TaskOptions.Builder.withUrl("/tasks/index/cran/fetchUpdates"));

    LOGGER.info("Enqueued task to check for updated CRAN packages: " + taskHandle.getName());

    return Response.ok().build();
  }


  @POST
  @Path("fetchUpdates")
  public Response fetchCranUpdates() {

    LocalDate threshold = new LocalDate().minusMonths(1);

    LOGGER.info("Fetching packages updated since: " + threshold);

    Queue queue = QueueFactory.getQueue("cran-fetch");
    List<String> recentlyUpdatedPackages = CRAN.fetchUpdatedPackageList(threshold);

    LOGGER.info("Found " + recentlyUpdatedPackages.size() + " recently updated packages.");

    for(String updatedPackage : recentlyUpdatedPackages) {
      queue.add(TaskOptions.Builder
          .withUrl("/tasks/index/cran/updatePackage")
          .param("packageName", updatedPackage));
    }

    return Response.ok().build();
  }

  @POST
  @Path("updatePackage")
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

      // archive the source to GCS
      archiveCranSourceToGcs(packageVersionId);
      PackageRegistrationTasks.enqueue(packageVersionId);
      SourceIndexTasks.enqueuePackageForSourceIndexing(packageVersionId);
    }
    return Response.ok().build();
  }

  @POST
  @Path("fetchArchives")
  public Response fetchArchives() {
    Iterable<Key<Package>> packages = ObjectifyService.ofy().load().type(Package.class)
        .chunk(1000)
        .keys()
        .iterable();

    Queue cranFetchQueue = QueueFactory.getQueue(CRAN_FETCH_QUEUE);

    for (Key<Package> aPackage : packages) {
      PackageId id = PackageId.valueOf(aPackage.getName());
      if(id.getGroupId().equals("org.renjin.cran")) {
        cranFetchQueue.add(TaskOptions.Builder
            .withUrl("/tasks/index/cran/fetchArchivedVersions")
            .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3))
            .param("packageName", id.getPackageName()));
      }
    }
    return Response.ok().entity("QUEUED.").build();
  }


  @POST
  @Path("fetchArchivedVersions")
  public Response fetchCranArchives(@FormParam("packageName") String packageName) throws IOException {
    final PackageId packageId = new PackageId("org.renjin.cran", packageName);
    final Set<PackageVersionId> versions = CRAN.getArchivedVersionList(packageId.getPackageName());

    LOGGER.info("Found " + versions.size() + " archived version(s)...");


    // See which versions we're missing
    List<Key<PackageVersion>> keys = Lists.newArrayList();
    for (PackageVersionId packageVersionId : versions) {
      keys.add(PackageVersion.key(packageVersionId));
    }
    Map<Key<PackageVersion>, PackageVersion> packageVersions = ObjectifyService.ofy().load().keys(keys);

    Queue cranFetchQueue = QueueFactory.getQueue(CRAN_FETCH_QUEUE);

    for (Key<PackageVersion> key : keys) {
      PackageVersionId packageVersionId = PackageVersion.idOf(key);
      if(packageVersions.containsKey(key)) {
        LOGGER.info(packageVersionId + ": already loaded.");
      } else {
        LOGGER.info(packageVersionId + ": queuing package fetch.");
        cranFetchQueue.add(TaskOptions.Builder
            .withUrl("/tasks/index/cran/fetchArchivedVersion")
            .param("packageVersion", packageVersionId.toString()));
      }
    }
    return Response.ok().build();
  }

  @POST
  @Path("fetchArchivedVersion")
  public Response fetchCranArchive(@FormParam("packageVersion") String packageVersion) throws IOException {
    PackageVersionId packageVersionId = new PackageVersionId(packageVersion);
    archiveCranSourceToGcs(packageVersionId);
    PackageRegistrationTasks.enqueue(packageVersionId);
    return Response.ok().build();
  }

  private void archiveCranSourceToGcs(PackageVersionId pvid) throws IOException {
    URL url = CRAN.sourceUrl(pvid.getPackageName(), pvid.getVersionString());
    LOGGER.info("Fetching source from " + url);

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int responseCode = urlConnection.getResponseCode();

    if(responseCode == 200) {
      PackageRegistrationTasks.archiveSource(pvid, urlConnection);

    } else if(responseCode == 404) {
      LOGGER.warning("Source URL not found, trying archive...");
      URL archiveUrl = CRAN.archivedSourceUrl(pvid.getPackageName(), pvid.getVersionString());
      urlConnection = (HttpURLConnection) archiveUrl.openConnection();
      responseCode = urlConnection.getResponseCode();
      if(responseCode != 200) {
        LOGGER.severe("Error fetching " + archiveUrl + ": " + responseCode);
      }
      PackageRegistrationTasks.archiveSource(pvid, urlConnection);

    } else {
      LOGGER.severe("Error fetching source: " + responseCode);
      throw new WebApplicationException(Response.Status.ACCEPTED);
    }
  }
}
