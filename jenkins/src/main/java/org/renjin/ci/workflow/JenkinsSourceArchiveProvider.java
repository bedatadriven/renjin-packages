package org.renjin.ci.workflow;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.AbortException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.SourceArchiveProvider;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.zip.GZIPInputStream;

/**
 * Provides source archives using the Google Cloud Storage API and credentials
 * stored within Jenkin's credentials.
 */
public class JenkinsSourceArchiveProvider implements SourceArchiveProvider {


  /**
   * Fetches credentials from the Google OAuth Plugin.
   */
  private Credential fetchCredentials() throws IOException {
    GoogleRobotCredentials credentials = GoogleRobotCredentials.getById("renjinci");
    if(credentials == null) {
      throw new AbortException("No service key credential available for project renjin ci");
    }
    Credential googleCredential;
    try {
      googleCredential = credentials.getGoogleCredential(new StorageRequirements());
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
    return googleCredential;
  }

  @Override
  public TarArchiveInputStream openSourceArchive(PackageVersionId packageVersionId) throws IOException {

    Storage storage = new Storage.Builder(new NetHttpTransport(), new JacksonFactory(), fetchCredentials())
        .setApplicationName("Renjin CI")
        .build();

    Storage.Objects.Get request = storage.objects().get(
        StorageKeys.PACKAGE_SOURCE_BUCKET,
        StorageKeys.packageSource(packageVersionId));

    InputStream inputStream = request.executeMediaAsInputStream();
    return new TarArchiveInputStream(new GZIPInputStream(inputStream));
  }


}
