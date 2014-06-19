package org.renjin.build.fetch;

import com.google.appengine.tools.cloudstorage.*;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.model.RPackageVersion;

import java.io.IOException;
import java.nio.channels.Channels;
import java.util.zip.GZIPInputStream;


public class AppEngineSourceArchiveProvider implements SourceArchiveProvider {


  private final GcsService gcsService =
    GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());


  @Override
  public TarArchiveInputStream openSourceArchive(RPackageVersion packageVersion) throws IOException {
    GcsFilename gcsFilename = CRAN.gcsFileName(packageVersion.getPackageName(), packageVersion.getVersion());
    GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(gcsFilename, 0, 1024 * 1024);
    return new TarArchiveInputStream(new GZIPInputStream(Channels.newInputStream(readChannel)));
  }
}
