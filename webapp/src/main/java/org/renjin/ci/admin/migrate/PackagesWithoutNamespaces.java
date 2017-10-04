package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.VoidWork;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.archive.SourceArchiveProvider;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PackagesWithoutNamespaces extends ForEachPackageVersion {

  private static final Logger LOGGER = Logger.getLogger(PackagesWithoutNamespaces.class.getName());

  @Override
  protected void apply(PackageVersionId packageVersionId) {

    if (!hasNamespace(packageVersionId)) {
      LOGGER.log(Level.SEVERE, packageVersionId + " has no namespace, disabling...");
      getContext().getCounter("disabled").increment(1);
      disablePackage(packageVersionId);
    }
  }

  private void disablePackage(final PackageVersionId packageVersionId) {
    PackageDatabase.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        PackageVersion pv = PackageDatabase.getPackageVersion(packageVersionId).get();
        pv.setDisabled(true);
        pv.setDisabledReason("Package does not have a NAMESPACE");
        PackageDatabase.save(pv).now();
      }
    });
  }

  private boolean hasNamespace(PackageVersionId packageVersionId) {

    String namespaceFile = packageVersionId.getPackageName() + "/NAMESPACE";

    SourceArchiveProvider archiveProvider = new AppEngineSourceArchiveProvider();
    try(TarArchiveInputStream tarIn = archiveProvider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry()) != null) {
        if(entry.getName().equals(namespaceFile)) {
          return true;
        }
      }
      return false;

    } catch (IOException e) {
      throw new RuntimeException("Failed to open source for " + packageVersionId);
    }
  }

}
