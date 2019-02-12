package org.renjin.ci.repo.maven;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.repo.ArtifactResource;

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
  public Response register(@FormParam("renjinVersion") String renjinVersion, @FormParam("objectName") List<String> objectNames) {
    Queue queue = QueueFactory.getDefaultQueue();
    for (String objectName : objectNames) {
      queue.add(TaskOptions.Builder.withDefaults()
          .param("renjinVersion", renjinVersion)
          .param("objectName", objectName)
          .url("/repo/m2/task"));

      if(objectName.endsWith(".deb")) {
        queue.add(TaskOptions.Builder.withDefaults()
          .param("objectName", objectName)
          .url("/repo/apt"));
      }
    }
    return Response.ok().build();
  }

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

  @Path("{release}/{path:.+}")
  public ArtifactResource getResource(@PathParam("release") String release, @PathParam("path") String path) {

//    // Is a hash being requested?
//    String hash = null;
//    for (String hashExt : HASHES) {
//      if(path.endsWith("." + hashExt)) {
//        hash = hashExt;
//        path = path.substring(0, path.length() - hashExt.length() - 1);
//      }
//    }
//
//    // Find the filename
//    int filenameStart = path.lastIndexOf('/');
//    if(filenameStart == -1) {
//      throw new WebApplicationException(Response.Status.NOT_FOUND);
//    }
//    String filename = path.substring(filenameStart + 1);
//
//    // Handle special cases
//    if(filename.endsWith("maven-metadata.xml")) {
//      return new MetadataResource();
//    }
//
//    //

    return new ArtifactResource(release, path);
  }

}
