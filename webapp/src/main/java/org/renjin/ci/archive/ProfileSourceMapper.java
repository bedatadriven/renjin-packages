package org.renjin.ci.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.io.IOException;


public class ProfileSourceMapper extends ForEachPackageVersion {

  
  private SourceVisitor visitor;
  
  private transient AppEngineSourceArchiveProvider sourceProvider;

  public ProfileSourceMapper(SourceVisitor visitor) {
    this.visitor = visitor;
  }

  @Override
  public void beginSlice() {
    this.sourceProvider = new AppEngineSourceArchiveProvider();
  }

  @Override
  protected void apply(PackageVersionId packageVersionId) {


    try(TarArchiveInputStream tarIn = sourceProvider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry())!=null) {
        visitor.visit(entry, tarIn);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
