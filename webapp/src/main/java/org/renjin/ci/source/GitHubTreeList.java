package org.renjin.ci.source;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.urlfetch.HTTPResponse;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Response from Github's tree list
 */
public class GitHubTreeList {

  private final List<String> paths = new ArrayList<>();

  public List<String> getPaths() {
    return paths;
  }

  public static GitHubTreeList fetch(String repo, String commitSha1) throws IOException {
    URL treeListUrl = new URL(String.format("https://github.com/%s/tree-list/%s", repo, commitSha1));
    HttpURLConnection connection = (HttpURLConnection) treeListUrl.openConnection();
    connection.setRequestProperty("Accept", "application/json");
    try(InputStream input = connection.getInputStream()) {
      ObjectMapper objectMapper = new ObjectMapper();

      return objectMapper.readValue(input, GitHubTreeList.class);
    }
  }
}
