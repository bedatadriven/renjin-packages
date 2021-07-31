package org.renjin.ci.repo;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.cloudstorage.*;
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

  public static final MemcacheService MEMCACHE_SERVICE = MemcacheServiceFactory.getMemcacheService();

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

    LOGGER.info("Directory listing for prefix '" + path + "'");

    String cachedHtml = tryGetCachedDirectoryListing(path);
    if(cachedHtml != null) {
      return Response.status(200)
              .type("text/html")
              .entity(cachedHtml)
              .build();
    }

    ListResult result = GCS_SERVICE.list("renjin-nexus-archive", new ListOptions.Builder()
            .setPrefix(path)
            .setRecursive(false)
            .build());

    if(!result.hasNext()) {
      return Response.status(Response.Status.NOT_FOUND).entity("<h1>Not found</h1>").build();
    }

    StringBuilder html = new StringBuilder();
    html.append("<h1>").append("Index of ")
            .append(HtmlEscapers.htmlEscaper().escape(path))
            .append("</h1>\n");

    html.append("<table><tr><th align=left>Name</th><th align=left>Last modified</th><th align=right>Size</th></tr>\n");
    if(path.length() > 1) {
      html.append("<tr><td><a href=\"../\">Parent Directory</a></td>\n");
    }
    result.forEachRemaining(item -> {
      String name = filenameOf(item.getName());
      String escapedName = HtmlEscapers.htmlEscaper().escape(name);
      html.append("<tr>");
      html.append("<td><a href=\"").append(escapedName).append("\">").append(escapedName).append("</a></td>\n");
      html.append("<td>");
      if(item.getLastModified() != null) {
        html.append(item.getLastModified());
      }
      html.append("</td>");
      html.append("<td align=right>");
      if(!item.isDirectory()) {
        html.append(item.getLength());
      }
      html.append("</td>");
      html.append("</tr>");
    });
    html.append("</table>");

    String htmlString = html.toString();

    try {
      MEMCACHE_SERVICE.put(memcacheDirectoryKey(path), htmlString, Expiration.byDeltaSeconds(3600));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to cache directory listing for " + path, e);
    }

    return Response.status(200)
      .type("text/html")
      .entity(htmlString)
      .build();
  }

  private String tryGetCachedDirectoryListing(String path) {
    try {
      return (String) MEMCACHE_SERVICE.get(memcacheDirectoryKey(path));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception fetching cached directory listing for " + path, e);
      return null;
    }
  }

  private String memcacheDirectoryKey(String path) {
    return "nexus:" + path;
  }

  static String filenameOf(String name) {
    int nameStart;
    if(name.endsWith("/")) {
      nameStart = name.lastIndexOf('/', name.length() - 2);
    } else {
      nameStart = name.lastIndexOf('/');
    }
    if(nameStart == -1) {
      return name;
    } else {
      return name.substring(nameStart + 1);
    }
  }

}
