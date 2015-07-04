package org.renjin.ci.index;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.joda.time.LocalDate;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;


public class CranTasks {

    public static final String CRAN_FETCH_QUEUE = "cran-fetch";

    private static final Logger LOGGER = Logger.getLogger(CranTasks.class.getName());

    @GET
    @Path("enqueue")
    public Response updateCran() {

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
        }
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
