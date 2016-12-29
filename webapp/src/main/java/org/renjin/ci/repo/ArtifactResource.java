package org.renjin.ci.repo;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.apache.commons.codec.binary.Base64;
import org.renjin.ci.datastore.Artifact;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Resource
 */
public class ArtifactResource {

  private static final Logger LOGGER = Logger.getLogger(ArtifactResource.class.getName());

  private String release;
  private String path;

  public ArtifactResource(String release, String path) {
    this.release = release;
    this.path = path;
  }

  @GET
  public Response get() {

    LOGGER.info("GET (" + release + ") " + path);



    Artifact artifact = ObjectifyService.ofy().load().key(Key.create(Artifact.class, path)).now();
    if(artifact == null) {
      return Response.status(404).build();
    }

    return Response.ok(artifact.getContent().getBytes()).build();
  }

  @PUT
  public Response put(@HeaderParam("Authorization") String authorization, byte[] content) {

    LOGGER.info("PUT (" + release + ") " + path);

    if (Strings.isNullOrEmpty(authorization) || authorization.startsWith("Basic ")) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .header("WWW-Authenticate", "Basic realm=\"User Visible Realm\"")
          .build();
    }

    //Decode username and password
    LOGGER.info("Authorization header: " + authorization);
    String encodedUserPassword = authorization.substring("Basic ".length());
    String usernameAndPassword = new String(Base64.decodeBase64(encodedUserPassword.getBytes()));



    //Split username and password tokens
    final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
    final String username = tokenizer.nextToken();
    final String password = tokenizer.nextToken();

    LOGGER.info("Upload request: " + username + " / " + password);

    //Verifying Username and password
    if(!username.equals("deploy") || !password.equals("XYZ123")) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    Artifact artifact = new Artifact();
    artifact.setPath(path);
    artifact.setRelease(release);
    artifact.setContent(new Blob(content));

    ObjectifyService.ofy().save().entity(artifact);

    return Response.ok().build();
  }

}
