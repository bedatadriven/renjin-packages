package org.renjin.ci.repo;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
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
  public Response getRootListing(@Context UriInfo uri) throws IOException {
    if(!uri.getPath().endsWith("/")) {
      return redirectToDirectory(uri);
    }
    return getDirectoryListing("");
  }

  @GET
  @Path("{path:.+}")
  public Response getArtifact(@Context UriInfo uri, @PathParam("path") String path) throws IOException {

    LOGGER.log(Level.INFO, "Looking for artifact " + path);

    if(path.endsWith("/")) {
      return getDirectoryListing(path);
    }

    GcsFileMetadata metadata;
    try {
      metadata = GCS_SERVICE.getMetadata(   new GcsFilename("renjin-nexus-archive", path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if(metadata == null) {
      if(isDirectory(path)) {
        return redirectToDirectory(uri);
      }
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

  private Response redirectToDirectory(UriInfo uri) {
    URI directoryUri = uri.getAbsolutePathBuilder().replacePath(uri.getPath() + "/").build();
    return Response.status(Response.Status.MOVED_PERMANENTLY).location(directoryUri).build();
  }

  private boolean isDirectory(String path) throws IOException {
    ListResult result = GCS_SERVICE.list("renjin-nexus-archive", new ListOptions.Builder()
            .setPrefix("path")
            .setRecursive(false)
            .build());
    return result.hasNext();
  }

  private Response notFound() {
    return Response.status(404)
      .type("text/html")
      .entity("<h1>Not found</h1>")
      .build();
  }

  private Response getDirectoryListing(String path) throws IOException {
    ListResult result = GCS_SERVICE.list("renjin-nexus-archive", new ListOptions.Builder()
            .setPrefix("path")
            .setRecursive(false)
            .build());

    if(!result.hasNext()) {
      return Response.status(Response.Status.NOT_FOUND).entity("<h1>Not found</h1>").build();
    }

    Escaper escaper = HtmlEscapers.htmlEscaper();
    StringBuilder html = new StringBuilder();
    html.append("<h1>").append("Index of ").append(escaper.escape(path)).append("</h1>\n");
    html.append("<table><tr><th>Name</th><th>Last modified</th><th>Size</th></tr>\n");
    result.forEachRemaining(item -> {
      String escapedName = escaper.escape(item.getName());
      if(item.isDirectory()) {
        escapedName += "/";
      }
      html.append("<tr>");
      html.append("<td><a href=\"").append(escapedName).append("\">").append(escapedName).append("</a></td>\n");
      html.append("<td>").append(item.getLastModified().toString()).append("</td>\n");
      if(!item.isDirectory()) {
        html.append("<td>").append(item.getLength()).append("</td>");
      }
      html.append("</tr>");
    });
    html.append("</table>");

    return Response.status(200)
      .type("text/html")
      .entity(html.toString())
      .build();
  }

}
