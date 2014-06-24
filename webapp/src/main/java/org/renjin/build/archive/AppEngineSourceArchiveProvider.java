package org.renjin.build.archive;

import com.google.appengine.tools.cloudstorage.*;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.model.RPackageVersion;
import org.renjin.build.storage.StorageKeys;
import org.renjin.build.tasks.SourceArchiveProvider;
import org.renjin.build.tasks.cran.CRAN;

import java.io.IOException;
import java.nio.channels.Channels;
import java.util.zip.GZIPInputStream;


public class AppEngineSourceArchiveProvider implements SourceArchiveProvider {

  private final GcsService gcsService =
    GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());


  @Override
  public TarArchiveInputStream openSourceArchive(PackageVersionId pvid) throws IOException {

    GcsFilename filename = new GcsFilename(StorageKeys.BUCKET_NAME,
        StorageKeys.packageSource(pvid.getGroupId(), pvid.getPackageName(), pvid.getSourceVersion()));

    GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(filename, 0, 1024 * 1024);

    return new TarArchiveInputStream(new GZIPInputStream(Channels.newInputStream(readChannel)));
  }
}
