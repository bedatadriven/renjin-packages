package org.renjin.ci.repo.maven;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.repo.ArtifactResource;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Path("/repo/m2")
public class MavenRepository {

  private Logger LOGGER = Logger.getLogger(MavenRepository.class.getName());

  private List<String> HASHES = Lists.newArrayList("md5", "sha1");

  @POST
  @Path("task")
  public Response registerTask(@HeaderParam("X-AppEngine-QueueName") String queueName,
                               @FormParam("renjinVersion") String renjinVersion,
                               @FormParam("objectName") String objectName) {


    String[] objectNameParts = objectName.split("/");
    String groupId = objectNameParts[0];
    String artifactId = objectNameParts[1];
    String version = objectNameParts[2];
    String filename = objectNameParts[3]
        .replaceFirst("pom\\.xml", artifactId + "-" + version + ".pom");

    MavenArtifact artifact = new MavenArtifact();
    artifact.setObjectName(objectName);
    artifact.setGroupArtifactPath(groupId.replace('.', '/') + "/" + artifactId);
    artifact.setFilename(filename);
    artifact.setLastModified(new Date());

    PackageDatabase.ofy().save().entity(artifact).now();

    return Response.ok().build();
  }

  @GET
  @Path("3.5-beta/{path:.+}")
  public Response getBetaArtifact(@PathParam("path") String path) {

    String gsPath = "/gs/" + StorageKeys.REPO_BUCKET + "/m2/3.5-beta/" + path;
    BlobKey blobKey = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gsPath);

    LOGGER.info("Serving " + gsPath);

    return Response.ok()
        .header("X-AppEngine-BlobKey", blobKey.getKeyString())
        .build();
  }

}
