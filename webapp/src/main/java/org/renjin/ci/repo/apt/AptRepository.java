package org.renjin.ci.repo.apt;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.tools.cloudstorage.*;
import com.googlecode.objectify.Key;
import org.bouncycastle.openpgp.PGPException;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

@Path("/repo/apt")
public class AptRepository {

  private static final Logger LOGGER = Logger.getLogger(AptRepository.class.getName());

  @GET
  @Path("ingest")
  public Response ingestRelease(@QueryParam("renjinVersion") String renjinVersion) throws IOException, PGPException {

    URL archiveUrl = new URL(String.format("https://nexus.bedatadriven.com/content/groups/public/org/renjin/renjin-debian-package/%s/renjin-debian-package-%s.deb",
        renjinVersion,
        renjinVersion));

    URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();

    HTTPResponse response = fetchService.fetch(archiveUrl);

    String objectName = "debian/renjin-" + renjinVersion + ".deb";
    GcsFilename debFilename = new GcsFilename(StorageKeys.ARTIFACTS_BUCKET, objectName);

    GcsService gcsService =
        GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    gcsService.createOrReplace(debFilename, new GcsFileOptions.Builder().build(),
        ByteBuffer.wrap(response.getContent()));

    return ingestArtifact(objectName);

  }


  @POST
  public Response ingestArtifact(@FormParam("objectName") String objectName) throws IOException, PGPException {

    AptPackageParser parser = new AptPackageParser();
    AptArtifact artifact = parser.parsePackage(objectName);

    AptPackage aptPackage = new AptPackage();
    aptPackage.setName(artifact.getName());
    aptPackage.setLatestVersion(artifact.getVersion());
    aptPackage.setLatestArtifact(Key.create(artifact));

    PackageDatabase.ofy().save().entities(artifact, aptPackage).now();

    index();

    return Response.ok().build();
  }

  public Response index() throws IOException, PGPException {

    LOGGER.info("Rebuilding index...");

    ControlFileBuilder packageIndex = new ControlFileBuilder();

    String repositoryVersion = "1.0";

    for (AptPackage aptPackage : PackageDatabase.ofy().load().type(AptPackage.class).iterable()) {

      LOGGER.info("Indexing package " + aptPackage.getName());

      AptArtifact artifact = PackageDatabase.ofy().load().key(aptPackage.getLatestArtifact()).now();


      if(aptPackage.getName().equals("Renjin")) {
        repositoryVersion = aptPackage.getLatestVersion();
      }

      packageIndex.write(artifact.getControlFile());
      packageIndex.writeField("Filename", "pool/" + artifact.getFilename());
      for (AptHash hash : artifact.getHashes()) {
        packageIndex.writeField(hash.getName(), hash.getHash());
      }
      packageIndex.writeField("Size", artifact.getSize());
    }

    ControlFileBuilder releaseIndex = new ControlFileBuilder();
      releaseIndex.writeField("Origin", "BeDataDriven BV");
      releaseIndex.writeField("Label", "Renjin Stable Repository");
      releaseIndex.writeField("Suite", "stable");
      releaseIndex.writeField("Codename", "stable");
      releaseIndex.writeField("Version", repositoryVersion);
      releaseIndex.writeField("Date", ZonedDateTime.now(ZoneOffset.UTC));
      releaseIndex.writeField("Architectures", "amd64 i386");
      releaseIndex.writeField("Components", "main");
      releaseIndex.writeField("Description", "Renjin's latest stable builds");

    releaseIndex.write(
        new PackageIndex("main/binary-amd64/Packages", packageIndex.toString()),
        new PackageIndex("main/binary-i386/Packages", packageIndex.toString()));

    AptSigner signer = AptSigner.fromGCS();

    AptDist release = new AptDist();
    release.setId("stable");
    release.setKeyId(signer.getKeyId());
    release.setPackageIndex(packageIndex.toString());
    release.setReleaseIndex(releaseIndex.toString());
    release.setReleaseIndexSigned(signer.sign(releaseIndex.toString(), true));
    release.setReleaseIndexSignature(signer.sign(releaseIndex.toString(), false));

    PgpKey key = new PgpKey();
    key.setId(signer.getKeyId());
    key.setPublicKey(signer.getPublicKeyArmored());

    PackageDatabase.ofy().save().entities(release, key).now();

    return Response.ok().build();
  }

  private AptDist findDist(String distName) {
    return PackageDatabase.ofy().load().key(Key.create(AptDist.class, distName)).now();
  }

  @GET
  @Path("dists/{dist}/InRelease")
  @Produces(MediaType.TEXT_PLAIN)
  public String releaseSignedIndex(@PathParam("dist") String distName) {
    return findDist(distName).getReleaseIndexSigned();
  }

  @GET
  @Path("dists/{dist}/Release")
  @Produces(MediaType.TEXT_PLAIN)
  public String releaseIndex(@PathParam("dist") String distName) {
    return findDist(distName).getReleaseIndex();
  }

  @GET
  @Path("dists/{dist}/Release.gpg")
  @Produces(MediaType.TEXT_PLAIN)
  public String releaseIndexSignature(@PathParam("dist") String distName) {
    return findDist(distName).getReleaseIndexSignature();
  }

  @GET
  @Path("dists/{dist}/main/{arch}/Packages")
  @Produces(MediaType.TEXT_PLAIN)
  public String packagesIndex(@PathParam("dist") String distName, @PathParam("arch") String arch) {
    return findDist(distName).getPackageIndex();
  }

  @GET
  @Path("pool/{filename}")
  public Response artifact(@PathParam("filename") String filename) {
    AptArtifact artifact = PackageDatabase.ofy().load().key(Key.create(AptArtifact.class, filename)).now();
    BlobKey blobKey = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(
        "/gs/" + StorageKeys.ARTIFACTS_BUCKET + "/deb/" + artifact.getObjectName());

    return Response.ok()
        .header("X-AppEngine-BlobKey", blobKey.getKeyString())
        .build();
  }

}
