package org.renjin.repo.task;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.repo.model.RPackageVersion;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;


public class RemoteSourceArchiveProvider implements SourceArchiveProvider {
  @Override
  public TarArchiveInputStream openSourceArchive(RPackageVersion version) throws IOException {


    URL url = new URL("http://commondatastorage.googleapis.com/package-sources/cran/" +
      version.getPackageName() + "_" + version.getVersion() + ".tar.gz");

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    int code = connection.getResponseCode();
    if(code != 200) {
      throw new IOException("Could not fetch source " + url + ": " + code);
    }
    return new TarArchiveInputStream(
      new GZIPInputStream(
        connection.getInputStream()));
  }
}
