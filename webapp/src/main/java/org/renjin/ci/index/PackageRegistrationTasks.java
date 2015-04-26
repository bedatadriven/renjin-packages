package org.renjin.ci.index;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.archive.SourceArchiveProvider;
import org.renjin.ci.build.PackageCheckQueue;
import org.renjin.ci.index.dependencies.DependencyResolver;
import org.renjin.ci.model.*;
import org.renjin.ci.model.Package;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers a new PackageVersion in our database
 */
@Path("/tasks/register")
public class PackageRegistrationTasks {

  private static final Logger LOGGER = Logger.getLogger(PackageRegistrationTasks.class.getName());

  
  
  public static void enqueue(PackageVersionId packageVersionId) {
    // Register the package version in our database
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl("/tasks/register").param("packageVersionId", packageVersionId.toString()));
  }
  
  public static void archiveSource(PackageVersionId packageVersionId, String sourceUrl) throws IOException {

    HTTPRequest request = new HTTPRequest(new URL(sourceUrl), HTTPMethod.GET, 
            FetchOptions.Builder.followRedirects().setDeadline(60d * 5d));
  
    URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();
    HTTPResponse response = fetchService.fetch(request);
    
    if(response.getResponseCode() == 200) {
      archiveSource(packageVersionId, new ByteArrayInputStream(response.getContent()));
    } else {
      throw new IOException("HTTP Status Code " + response.getResponseCode() + " received while fetching " + sourceUrl);
    }
  }

  public static void archiveSource(PackageVersionId packageVersionId, HttpURLConnection urlConnection) throws IOException {
    try(InputStream inputStream = urlConnection.getInputStream()) {
      archiveSource(packageVersionId, inputStream);
    }
  }



  private static void archiveSource(PackageVersionId packageVersionId, InputStream inputStream) throws IOException {
    GcsFilename filename = new GcsFilename(StorageKeys.PACKAGE_SOURCE_BUCKET, StorageKeys.packageSource(packageVersionId));
    LOGGER.info("Storing source archive at " + filename);


    GcsService gcsService =
            GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    GcsOutputChannel outputChannel =
            gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());

    try(OutputStream outputStream = Channels.newOutputStream(outputChannel)) {

      ByteStreams.copy(inputStream, outputStream);

    }
  }

  /**
   *
   1. Create a new PackageVersion entity from the DESCRIPTION File
   2. Create a new Package entity if one does not exist already
   3. If this PackageVersion is the newer than any existing PackageVersion of this Package, run the PromoteLatestVersion task
   4. Set PackageVersion.dependencies to the result of the ResolveDependencyVersions
   5. If package is not an orphan, run the EnqueuePVS algorithm for the latest release
   version of Renjin
   */
  @POST
  public Response run(@FormParam("packageVersionId") String packageVersionId) throws Exception {

    LOGGER.log(Level.INFO, "Registering new package version " + packageVersionId);
    
    PackageVersionId pvid = PackageVersionId.fromTriplet(packageVersionId);

    try {
      // Read the DESCRIPTION file from the .tar.gz source archive
      String descriptionSource = readPackageDescription(pvid);

      // create the new entity for this packageVersion
      PackageVersion packageVersion = register(pvid, descriptionSource);

      ObjectifyService.ofy().save().entity(packageVersion);
      
      // Update the package entity if this is the latest version
      maybeUpdatePackage(packageVersion);

    } catch(InvalidPackageException e) {
      LOGGER.log(Level.SEVERE, "Could not accept package " + pvid, e);
    }

    return Response.ok().build();
  }

  private void maybeUpdatePackage(final PackageVersion packageVersion) {
    ObjectifyService.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        PackageVersionId newPvid = packageVersion.getPackageVersionId();
        Package packageEntity = PackageDatabase.getPackageOf(newPvid).orNull();
        if(packageEntity == null) {
          packageEntity = new Package(packageVersion.getGroupId(), newPvid.getPackageName());
        }
        
        if(packageEntity.getLatestVersion() == null || 
              newPvid.isNewer(packageEntity.getLatestVersionId())) {
          
          LOGGER.info("Updating package entry with new latest version...");
          
          PackageDescription description = packageVersion.parseDescription();
          packageEntity.setTitle(description.getTitle());
          packageEntity.setLatestVersion(newPvid.getVersionString());
          
          ObjectifyService.ofy().save().entity(packageEntity);
          
          PackageSearchIndex.updateIndex(packageVersion);
        } else {
          
          LOGGER.info("Package " + packageEntity.getId() + " is already @ " +
                  packageEntity.getLatestVersion() + ", not updating.");
          
        }
      }
    });
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
    SourceArchiveProvider sourceArchiveProvider = new AppEngineSourceArchiveProvider();
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
