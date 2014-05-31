package org.renjin.build.task;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.model.RPackageVersion;

import java.io.IOException;

public interface SourceArchiveProvider {

  TarArchiveInputStream openSourceArchive(RPackageVersion version) throws IOException;


}
