package org.renjin.ci.index;

import com.google.appengine.tools.cloudstorage.*;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;
import com.google.common.io.ByteStreams;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.logging.Logger;

/**
 * Fetches the latest version of a package in CRAN
 */
public class FetchCranPackage extends Job1<Void, String> {

  private static final Logger LOGGER = Logger.getLogger(FetchCranPackage.class.getName());

  private final GcsService gcsService =
      GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

  @Override
  public Value<Void> run(String packageName) throws Exception {

    LOGGER.info("Fetching latest version of " + packageName + " from CRAN");

    // get the latest version from CRAN
    String version = CRAN.fetchLatestPackageVersion(packageName);
    PackageVersionId packageVersionId = new PackageVersionId("org.renjin.cran", packageName, version);

    LOGGER.info("Latest version is " + packageVersionId);

    // Do we have this already?
    if(PackageDatabase.getPackageVersion(packageVersionId).isPresent()) {

      LOGGER.info("Already present in database");

    } else {

      LOGGER.info("New version: Fetching source...");

      // archive the source to
      archiveSourceToGcs(packageName, version);

      // Register the package version in our database

      waitFor(futureCall(new RegisterPackageVersion(), immediate(packageVersionId)));

    }

    return null;
  }

  private GcsFilename archiveSourceToGcs(String packageName, String version) throws IOException {
    URL url = CRAN.sourceUrl(packageName, version);
    LOGGER.info("Fetching source from " + url);

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int responseCode = urlConnection.getResponseCode();

    if(responseCode == 200) {
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else if(responseCode == 404) {
      LOGGER.warning("Source URL not found, trying archive...");
      URL archiveUrl = CRAN.archivedSourceUrl(packageName, version);
      urlConnection = (HttpURLConnection) archiveUrl.openConnection();
      responseCode = urlConnection.getResponseCode();
      if(responseCode != 200) {
        LOGGER.severe("Error fetching " + archiveUrl + ": " + responseCode);
      }
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else {
      LOGGER.severe("Error fetching source: " + responseCode);
      throw new WebApplicationException(Response.Status.ACCEPTED);
    }
  }

  private GcsFilename archiveSourceToGcs(HttpURLConnection urlConnection, String packageName, String version) throws IOException {

    GcsFilename filename = CRAN.gcsFileName(packageName, version);
    LOGGER.info("Storing source archive at " + filename);

    GcsOutputChannel outputChannel =
        gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());

    try(
        OutputStream outputStream = Channels.newOutputStream(outputChannel);
        InputStream inputStream = urlConnection.getInputStream()) {

      ByteStreams.copy(inputStream, outputStream);

    }
    return filename;
  }

}
