package org.renjin.build.tasks;


import com.google.common.io.Closeables;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.model.RPackageVersion;

import java.io.IOException;

public class SourceCrawler {

  private final SourceArchiveProvider sourceArchiveProvider;
  private final SourceVisitor visitor;

  public SourceCrawler(SourceArchiveProvider sourceArchiveProvider, SourceVisitor visitor) {
    this.sourceArchiveProvider = sourceArchiveProvider;
    this.visitor = visitor;
  }

  public void crawl(PackageVersionId packageVersion) throws IOException {

    TarArchiveInputStream tarIn = sourceArchiveProvider.openSourceArchive(packageVersion);
    try {
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry())!=null) {
        visitor.visit(entry, tarIn);
      }
    } finally {
      Closeables.closeQuietly(tarIn);
    }
  }
}
