package org.renjin.ci.archive;

import com.google.appengine.tools.cloudstorage.*;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;

import java.io.IOException;
import java.nio.channels.Channels;
import java.util.zip.GZIPInputStream;


public class AppEngineSourceArchiveProvider implements SourceArchiveProvider {

  private final GcsService gcsService =
    GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());


  @Override
  public TarArchiveInputStream openSourceArchive(PackageVersionId pvid) throws IOException {

    GcsFilename filename = new GcsFilename(StorageKeys.PACKAGE_SOURCE_BUCKET,
        StorageKeys.packageSource(pvid.getGroupId(), pvid.getPackageName(), pvid.getVersionString()));

    GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(filename, 0, 1024 * 1024);

    return new TarArchiveInputStream(new GZIPInputStream(Channels.newInputStream(readChannel)));
  }
}
