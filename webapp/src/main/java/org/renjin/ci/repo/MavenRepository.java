package org.renjin.ci.repo;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Path("/repo/m2")
public class MavenRepository {

  private Logger LOGGER = Logger.getLogger(MavenRepository.class.getName());

  private List<String> HASHES = Lists.newArrayList("md5", "sha1");

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
