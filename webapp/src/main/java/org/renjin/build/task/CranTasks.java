package org.renjin.build.task;


import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.joda.time.LocalDate;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.PackageDescription;
import org.renjin.build.model.RPackage;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Fetch the latest list of package versions from a CRAN mirror
 */
public class CranTasks {

  private final static Logger LOGGER = Logger.getLogger(CranTasks.class.getName());


  private final GcsService gcsService =
          GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());


  @GET
  @Path("updateIndex")
  public Response updateIndex() {

    EntityManager em = HibernateUtil.createEntityManager();
    try {
      Date lastUpdate = em
          .createQuery("select max(v.publicationDate) from RPackageVersion v", Date.class)
          .getSingleResult();

      List<String> updatedPackageNames = CRAN.fetchUpdatedPackageList(new LocalDate(lastUpdate));
      for(String updatedPackage : updatedPackageNames) {
        enqueueFetch(updatedPackage);
      }
    } finally {
      closeQuietly(em);
    }
    return Response.ok().build();
  }

  @Path("calculateDependencies")
  public CalculateDependenciesTask calculateDependencies() {
    return new CalculateDependenciesTask();
  }

  private void enqueueFetch(String packageName) {
    Queue queue = QueueFactory.getQueue("cran-fetch");
    queue.add(TaskOptions.Builder.withUrl("/tasks/cran/fetch")
            .param("packageName", packageName));
  }


  @POST
  @Path("fetch")
  public void fetchPackage(@FormParam("packageName") String packageName) throws IOException {

    String version = CRAN.fetchLatestPackageVersion(packageName);

    EntityManager em = HibernateUtil.createEntityManager();           
    try {
      em.getTransaction().begin();
  
      GcsFilename filename = archiveSourceToGcs(packageName, version);
  
      String pkgId = CRAN.packageId(packageName);
      RPackage pkg = em.find(RPackage.class, pkgId);
      if(pkg == null) {
        System.out.println("Registering new package " + packageName + " " + version);
  
        pkg = new RPackage();
        pkg.setId(pkgId);
        pkg.setName(packageName);
        em.persist(pkg);
      }
      String pkgVersionId = CRAN.packageVersionId(packageName, version);
      RPackageVersion pkgVersion = em.find(RPackageVersion.class, pkgVersionId);
      if(pkgVersion == null) {
        pkgVersion = new RPackageVersion();
        pkgVersion.setId(pkgVersionId);
        pkgVersion.setVersion(version);
        pkgVersion.setRPackage(pkg);
      }
      pkgVersion.setSourceDownloaded(true);
      pkgVersion.setDescription(readPackageDescription(packageName, filename));
  
      // Parse some things out of the DESCRIPTION file that we need
      PackageDescription description = PackageDescription.fromString(pkgVersion.getDescription());
  
      try {
        pkgVersion.setPublicationDate(description.getPublicationDate());
      } catch(Exception e) {
      }
      em.persist(pkgVersion);
      em.getTransaction().commit();
    } finally {
      em.close();
    }
  }

  private String readPackageDescription(String packageName, GcsFilename fileName) throws IOException {
    GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(fileName, 0, 1024 * 1024);
    try {
      TarArchiveInputStream tarIn = new TarArchiveInputStream(
              new GZIPInputStream(Channels.newInputStream(readChannel)));
      TarArchiveEntry entry;
      while((entry = tarIn.getNextTarEntry())!=null) {

        if(entry.getName().equals(packageName + "/DESCRIPTION")) {
          return new String(ByteStreams.toByteArray(tarIn), Charsets.UTF_8);
        }
      }
    } finally {
      readChannel.close();
    }
    throw new IOException("No description file");
  }

  private GcsFilename archiveSourceToGcs(String packageName, String version) throws IOException {
    URL url = CRAN.sourceUrl(packageName, version);
    LOGGER.info("Fetching source from " + url);

    HttpURLConnection urlConnection = getUrl(url);
    int responseCode = urlConnection.getResponseCode();

    if(responseCode == 200) {
      return archiveSourceToGcs(urlConnection, packageName, version);

    } else if(responseCode == 404) {
      LOGGER.warning("Source URL not found, trying archive...");
      URL archiveUrl = CRAN.archivedSourceUrl(packageName, version);
      urlConnection = getUrl(archiveUrl);
      responseCode = urlConnection.getResponseCode();
      if(responseCode != 200) {
        LOGGER.severe("Error fetching " + archiveUrl + ": " + responseCode);
      }

      return archiveSourceToGcs(urlConnection, packageName, version);

    } else {
      LOGGER.severe("Error fetching source: " + responseCode);
      throw new WebApplicationException(Response.Status.ACCEPTED);
    }
  }

  private GcsFilename archiveSourceToGcs(HttpURLConnection urlConnection, String packageName, String version) throws IOException {

    GcsFilename filename = CRAN.gcsFileName(packageName, version);
    LOGGER.info("Storing source archive at " + filename);

    GcsOutputChannel outputChannel =
            gcsService.createOrReplace(filename, GcsFileOptions.getDefaultInstance());

    try(
        OutputStream outputStream = Channels.newOutputStream(outputChannel);
        InputStream inputStream = urlConnection.getInputStream()) {

      ByteStreams.copy(inputStream, outputStream);

    }
    return filename;
  }

  private HttpURLConnection getUrl(URL url) throws IOException {
    HttpURLConnection urlConnection =  (HttpURLConnection) url.openConnection ();
    urlConnection.setRequestMethod("GET");
    urlConnection.connect() ;
    return urlConnection;
  }


  private void closeQuietly(EntityManager em) {
    try {
      em.close();
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Exception closing connection", e);
    }
  }
}
