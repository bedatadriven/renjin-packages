package org.renjin.ci.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;

public interface SourceArchiveProvider {

  TarArchiveInputStream openSourceArchive(PackageVersionId packageVersionId) throws IOException;


}
