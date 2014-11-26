package org.renjin.ci.index;

import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.googlecode.objectify.ObjectifyService;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.build.PackageCheckQueue;
import org.renjin.ci.index.dependencies.DependencyResolver;
import org.renjin.ci.index.dependencies.DependencySet;
import org.renjin.ci.model.*;
import org.renjin.ci.archive.SourceArchiveProvider;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers a new PackageVersion in our database
 */
public class RegisterPackageVersion extends Job1<PackageVersionId, PackageVersionId> {

  private static final Logger LOGGER = Logger.getLogger(RegisterPackageVersion.class.getName());

  SourceArchiveProvider sourceArchiveProvider = new AppEngineSourceArchiveProvider();

  /**
   *
   1. Create a new PackageVersion entity from the DESCRIPTION File
   2. Create a new Package entity if one does not exist already
   3. If this PackageVersion is the newer than any existing PackageVersion of this Package, run the PromoteLatestVersion task
   4. Set PackageVersion.dependencies to the result of the ResolveDependencyVersions
   5. If package is not orpahn, run the EnqueuePVS algorithm for the latest release
   version of Renjin
   */
  @Override
  public Value<PackageVersionId> run(PackageVersionId pvid) throws Exception {

    LOGGER.log(Level.INFO, "Registering new package version " + pvid);

    try {
      // Read the DESCRIPTION file from the .tar.gz source archive
      String descriptionSource = readPackageDescription(pvid);

      // create the new entity for this packageVersion
      PackageVersion packageVersion = register(pvid, descriptionSource);

      ObjectifyService.ofy().save().entity(packageVersion);

    } catch(InvalidPackageException e) {
      LOGGER.log(Level.SEVERE, "Could not accept package " + pvid, e);
    }

    return immediate(pvid);
  }

  @VisibleForTesting
  PackageVersion register(PackageVersionId packageVersionId, String descriptionSource) throws IOException, ParseException {

    PackageDescription description = PackageDescription.fromString(descriptionSource);

    // Resolve (versioned) dependencies
    DependencySet dependencySet = new DependencyResolver()
        .basedOnPublicationDateFrom(description)
        .resolveAll(description);

    // Create a new PackageVersion entity
    PackageVersion packageVersion = new PackageVersion(packageVersionId);
    packageVersion.setPublicationDate(description.getPublicationDate());
    packageVersion.setDescription(descriptionSource);
    packageVersion.setDependencies(dependencySet);

    // Create a status record for the current release
    PackageCheckQueue.createStatus(packageVersion, RenjinVersionId.RELEASE);

    return packageVersion;
  }

  /**
   *
   * @param packageVersionId packageVersionId
   * @return the String contents of the DESCRIPTION file
   * @throws IOException
   */
  private String readPackageDescription(PackageVersionId packageVersionId) throws InvalidPackageException, IOException {
    try(TarArchiveInputStream tarIn = sourceArchiveProvider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry()) != null) {

        if(entry.getName().equals(packageVersionId.getPackageName() + "/DESCRIPTION")) {
          String text = new String(ByteStreams.toByteArray(tarIn), Charsets.UTF_8);
          return text;
        }
      }
    }
    throw new InvalidPackageException("Missing DESCRIPTION file");
  }
}
