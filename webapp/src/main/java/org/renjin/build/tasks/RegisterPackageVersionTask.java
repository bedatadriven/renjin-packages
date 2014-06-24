package org.renjin.build.tasks;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.googlecode.objectify.ObjectifyService;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.build.archive.AppEngineSourceArchiveProvider;
import org.renjin.build.model.*;
import org.renjin.build.tasks.dependencies.DependencyResolver;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers a new PackageVersion in our database
 */
@Path(RegisterPackageVersionTask.PATH)
public class RegisterPackageVersionTask {

  public static final String PATH = "/tasks/registerPackVersion";

  private static final Logger LOGGER = Logger.getLogger(RegisterPackageVersionTask.class.getName());

  SourceArchiveProvider sourceArchiveProvider = new AppEngineSourceArchiveProvider();


  public static void queue(PackageVersionId packageVersionId) {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder
        .withUrl(PATH)
        .param("packageVersionId", packageVersionId.toString()));
  }

  /**
   *
   1. Create a new PackageVersion entity from the DESCRIPTION File
   2. Create a new Package entity if one does not exist already
   3. If this PackageVersion is the newer than any existing PackageVersion of this Package, run the PromoteLatestVersion task
   4. Set PackageVersion.dependencies to the result of the ResolveDependencyVersions
   5. If package is not orpahn, run the EnqueuePVS algorithm for the latest release
   version of Renjin
   */
  @POST
  public Response register(@FormParam("packageVersionId") String packageVersionId) throws IOException, ParseException {

    LOGGER.log(Level.INFO, "Registering new package version " + packageVersionId);

    try {
      PackageVersionId pvid = PackageVersionId.fromTriplet(packageVersionId);

      // Read the DESCRIPTION file from the .tar.gz source archive
      String descriptionSource = readPackageDescription(pvid);

      // create the new entity for this packageVersion
      PackageVersion packageVersion = register(pvid, descriptionSource);

      ObjectifyService.ofy().save().entity(packageVersion);


    } catch(InvalidPackageException e) {
      LOGGER.log(Level.SEVERE, "Could not accept package " + packageVersionId, e);
    }

    return Response.ok().build();
  }

  @VisibleForTesting
  PackageVersion register(PackageVersionId packageVersionId, String descriptionSource) throws IOException, ParseException {

    PackageDescription description = PackageDescription.fromString(descriptionSource);

    // Create a new PackageVersion entity
    PackageVersion packageVersion = new PackageVersion(packageVersionId);
    packageVersion.setPublicationDate(description.getPublicationDate());
    packageVersion.setDescription(descriptionSource);
    packageVersion.setDependencies(Sets.<PackageVersionId>newHashSet());

    // try to resolve dependencies:
    new ResolveDependenciesTask().resolveDependencies(packageVersion);

    packageVersion.setCompileDependenciesResolved(false);

    // queue this package for building / testing against the latest renjin
    // release
    new PackageCheckQueue().updateStatus(packageVersion, RenjinVersionId.RELEASE);

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
