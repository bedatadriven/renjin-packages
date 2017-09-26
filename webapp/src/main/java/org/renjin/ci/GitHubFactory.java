package org.renjin.ci;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpConnector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubFactory {

  private static class AppEngineGitHubConnector implements HttpConnector {

    @Override
    public HttpURLConnection connect(URL url) throws IOException {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("User-Agent", "Renjin CI");

      return connection;
    }
  }
  /**
   * Personal OAUTH token that provides read-only access to public repos
   */
  private static final String OAUTH_TOKEN = "ce8814c5a7468b95a353af728d343aa59cc44ca3";


  public static GitHub create() throws IOException {
    GitHub github = GitHub.connectUsingOAuth(OAUTH_TOKEN);
    github.setConnector(new AppEngineGitHubConnector());

    return github;
  }
}
