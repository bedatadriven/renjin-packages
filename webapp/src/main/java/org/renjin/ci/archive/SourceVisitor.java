package org.renjin.ci.archive;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.io.InputStream;

public abstract class SourceVisitor {

  public abstract void visit(TarArchiveEntry entry, InputStream inputStream) throws IOException;
  
  public final void accept(PackageVersionId id) {

    AppEngineSourceArchiveProvider sourceProvider = new AppEngineSourceArchiveProvider();
    
    try(TarArchiveInputStream tarIn = sourceProvider.openSourceArchive(id)) {
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry())!=null) {
        visit(entry, tarIn);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
}
