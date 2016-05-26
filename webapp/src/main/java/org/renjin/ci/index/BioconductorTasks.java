package org.renjin.ci.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.taskqueue.*;
import com.google.common.base.Optional;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Task end points for fetching and indexing bioconductor packages
 */
public class BioconductorTasks {
    
    private static final Logger LOGGER = Logger.getLogger(BioconductorTasks.class.getName());

    private static final String BIO_CONDUCTOR_QUEUE = "bioConductor-fetch";

    
    /**
     * Cron endpoint that enqueues the {@code fetchBioconductorUpdates()} task
     */
    @GET
    @Path("enqueue")
    public Response updateBioConductor(@QueryParam("release") String releaseNumber) {
        
        if(Strings.isNullOrEmpty(releaseNumber)) {
            releaseNumber = "3.2";
        }
        
        Queue queue = QueueFactory.getQueue(BIO_CONDUCTOR_QUEUE);
        TaskHandle taskHandle = queue.add(TaskOptions.Builder
            .withUrl("/tasks/index/bioc/fetchList")
            .param("bioconductorRelease", releaseNumber));

        LOGGER.info("Enqueued task to check for updated Bioconductor packages: " + taskHandle.getName());

        return Response.ok().build();
    }
    
    /**
     * Fetches the list of packages and their current version that are part of a given BioConductor release,
     * and enqueues a {@code checkBioConductorUpdate} for each package found.
     */
    @POST
    @Path("fetchList")
    public Response fetchBioconductorUpdates(
        @FormParam("bioconductorRelease") String releaseNumber) throws IOException {
        
        URL packageListUrl = new URL(String.format(
                "http://master.bioconductor.org/packages/json/%s/bioc/packages.json", releaseNumber));

        LOGGER.info("Fetching list of packages from " + packageListUrl);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode packageList = objectMapper.readTree(packageListUrl);

        Queue queue = QueueFactory.getQueue(BIO_CONDUCTOR_QUEUE);

        Iterator<String> packageIt = packageList.fieldNames();
        while(packageIt.hasNext()) {
            String packageName = packageIt.next();
            JsonNode packageNode = packageList.get(packageName);
            String version = packageNode.get("Version").asText();
            String sourceUrl = "http://master.bioconductor.org/packages/release/bioc/" + packageNode.get("source.ver").asText();

            queue.add(TaskOptions.Builder.withUrl("/tasks/index/bioc/updatePackage")
                    .param("bioconductorRelease", releaseNumber)
                    .param("packageName", packageName)
                    .param("packageVersion", version)
                    .param("sourceUrl", sourceUrl)
                    .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3)));

        }
        return Response.ok().build();
    }
    
    @POST
    @Path("updatePackage")
    public Response updatePackage(
            @FormParam("bioconductorRelease") String bioconductorRelease,
            @FormParam("packageName") String packageName,
            @FormParam("packageVersion") String packageVersion,
            @FormParam("sourceUrl") String sourceUrl) throws IOException {

        LOGGER.info("Checking if we have BioConductor package " + packageName + " @ " + packageVersion);


        PackageVersionId packageVersionId = new PackageVersionId("org.renjin.bioconductor", packageName, packageVersion);

        Optional<PackageVersion> result = PackageDatabase.getPackageVersion(packageVersionId);
        if(result.isPresent()) {
            LOGGER.info(packageVersionId + " is already present");

        } else {
            LOGGER.info("Ingesting new package version " + packageVersionId);

            PackageRegistrationTasks.archiveSource(packageVersionId, sourceUrl);
            PackageRegistrationTasks.enqueueBioconductor(bioconductorRelease, packageVersionId);

        }

        return Response.ok().build();
    }

}
