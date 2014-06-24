package org.renjin.build.tasks;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.model.RPackageVersion;

import java.io.IOException;

public interface SourceArchiveProvider {

  TarArchiveInputStream openSourceArchive(PackageVersionId packageVersionId) throws IOException;


}
