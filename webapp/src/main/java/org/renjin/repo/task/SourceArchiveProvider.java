package org.renjin.repo.task;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.repo.model.RPackageVersion;

import java.io.IOException;

public interface SourceArchiveProvider {

  TarArchiveInputStream openSourceArchive(RPackageVersion version) throws IOException;


}
