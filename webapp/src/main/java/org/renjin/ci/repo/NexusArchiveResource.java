package org.renjin.ci.repo;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves archived artifacts from the old nexus.bedatadriven.com
 */
@Path("content/groups/public")
public class NexusArchiveResource {

  private static final Logger LOGGER = Logger.getLogger(NexusArchiveResource.class.getName());

  public static final GcsService GCS_SERVICE = GcsServiceFactory.createGcsService();
  public static final BlobstoreService BLOBSTORE_SERVICE = BlobstoreServiceFactory.getBlobstoreService();

  @GET
  @Path("{path:.+}")
  public Response getArtifact(@PathParam("path") String path) {

    LOGGER.log(Level.INFO, "Looking for artifact " + path);

    if(path.endsWith("/")) {
      return getDirectoryListing(path);
    }

    GcsFileMetadata metadata;
    try {
      metadata = GCS_SERVICE.getMetadata(new GcsFilename("renjin-nexus-archive", path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if(metadata == null) {
      return notFound();
    }

    String blobPath = "/gs/renjin-nexus-archive/" + path;
    BlobKey blobKey = BLOBSTORE_SERVICE
      .createGsBlobKey(blobPath);

    LOGGER.log(Level.INFO, "Blobkey = -->" + blobPath + "<---");

    return Response.ok()
      .status(200)
      .header("X-AppEngine-BlobKey", blobKey.getKeyString())
      .build();
  }

  private Response notFound() {
    return Response.status(404)
      .type("text/html")
      .entity("<h1>Not found</h1>")
      .build();
  }

  private Response getDirectoryListing(String path) {
    return Response.status(404)
      .type("text/html")
      .entity("<h1>Directory listing disabled.</h1>")
      .build();
  }

}
