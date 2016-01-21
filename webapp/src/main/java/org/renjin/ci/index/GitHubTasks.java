package org.renjin.ci.index;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Fetches package data from github
 */
@Path("/tasks/github")
public class GitHubTasks {

  public static final String GITHUB_FETCH_QUEUE = "github-fetch";
  
  private static final Logger LOGGER = Logger.getLogger(GitHubTasks.class.getName());

  
  public static void enqueue(String owner, String repo) {
    QueueFactory.getQueue(GITHUB_FETCH_QUEUE).add(TaskOptions.Builder.withUrl("/tasks/github/update")
        .param("owner", owner)
        .param("repo", repo));
  }
  
  @POST
  @Path("update")
  public Response updatePackage(
      @FormParam("owner") String owner,
      @FormParam("repo") String repo) throws IOException, JSONException {

    PackageId packageId = PackageId.gitHub(owner, repo);

    LOGGER.info("Fetching known tags...");
    Set<String> loadedTags = PackageDatabase.getPackageVersionTags(packageId);

    LOGGER.info("Fetching releases from " + owner + "/" + repo);
    URL releasesUrl = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/releases");
    String releasesJson = Resources.toString(releasesUrl, Charsets.UTF_8);
    
    LOGGER.info("Releases: " + releasesJson);
    
    JSONArray releases = new JSONArray(releasesJson);

    for (int i = 0; i < releases.length(); i++) {
      JSONObject release = releases.getJSONObject(i);
      
      String tagName = release.getString("tag_name");
      
      if(!loadedTags.contains(tagName)) {
        QueueFactory.getQueue(GITHUB_FETCH_QUEUE).add(TaskOptions.Builder.withUrl("/tasks/github/fetchRelease")
        .param("owner", owner)
        .param("repo", repo)
        .param("tagName", tagName));
      }
    }
    
    return Response.ok().build();
  }

  @POST
  @Path("fetchRelease")
  public Response fetchRelease(
      @FormParam("owner") String owner,
      @FormParam("repo") String repo,
      @FormParam("tagName") String tagName) throws IOException, ParseException {
    
    // First fetch the DESCRIPTION file to get the real version number
    URL descriptionURL = new URL(String.format("https://raw.githubusercontent.com/%s/%s/%s/DESCRIPTION", owner, repo, tagName));
    String descriptionSource = Resources.toString(descriptionURL, Charsets.UTF_8);
    PackageDescription description = PackageDescription.fromString(descriptionSource);
    
    PackageVersionId packageVersionId = new PackageVersionId(PackageId.gitHub(owner, repo), description.getVersion());
    
    PackageRegistrationTasks.archiveSource(packageVersionId,
        String.format("https://github.com/%s/%s/archive/%s.tar.gz", owner, repo, tagName));

    PackageRegistrationTasks.enqueue(packageVersionId);
    
    return Response.ok().build();
  }
  

}
