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

  @GET
  @Path("stable/{path:.+}")
  public Response query(@PathParam("path") String path) {

    if(MavenMetadataRequest.matches(path)) {
      // TODO
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    MavenArtifactRequest artifactRequest = MavenArtifactRequest.parse(path);

    return Response.ok()
        .header("X-AppEngine-BlobKey", artifactRequest.getBlobKey().getKeyString())
        .build();
  }

}
