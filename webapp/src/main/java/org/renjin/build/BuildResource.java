package org.renjin.build;

import com.google.common.base.Optional;
import org.renjin.build.archive.GcsAppIdentityServiceUrlSigner;
import org.renjin.build.model.*;
import org.renjin.build.storage.StorageKeys;
import org.renjin.build.task.PackageBuildTask;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/build")
public class BuildResource {

  GcsAppIdentityServiceUrlSigner signer = new GcsAppIdentityServiceUrlSigner();

  @POST
  @Path("next")
  @Produces("application/json")
  public Response getNextBuild() throws Exception {
    Optional<PackageStatus> next = PackageDatabase.getNextReady();
    if(next.isPresent()) {
      return Response.ok(createNewBuild(next.get())).build();
    } else {
      return Response.status(Response.Status.NO_CONTENT).build();
    }
  }

  private PackageBuildTask createNewBuild(PackageStatus status) throws Exception {

    // get the next build number
    long buildNumber = PackageDatabase.newBuildNumber(status.getPackageVersionId());

    // create the new build entity
    PackageBuild build = new PackageBuild(status.getPackageVersionId(), buildNumber);
    build.setRenjinVersion(status.getRenjinVersionId().toString());
    build.setDependencies(status.getDependencies());
    ofy().save().entity(build);

    // load the description file and generate the pom for this build
    PackageVersion packageVersion = ofy().load().key(status.getPackageVersionId().key()).safe();
    PomBuilder pomBuilder = new PomBuilder(build, packageVersion.parseDescription());

    PackageBuildTask task = new PackageBuildTask();
    task.setPom(pomBuilder.getXml());
    task.setGroupId(build.getPackageVersionId().getGroupId());
    task.setPackageName(build.getPackageVersionId().getPackageName());
    task.setSourceVersion(build.getPackageVersionId().getSourceVersion());
    task.setSourceUrl(getSignedSourceUrl(build.getPackageVersionId()));
    return task;
  }

  private String getSignedSourceUrl(PackageVersionId versionId) throws Exception {
    return signer.getSignedUrl("GET", StorageKeys.BUCKET_NAME,
        StorageKeys.packageSource(versionId.getGroupId(), versionId.getPackageName(), versionId.getSourceVersion()));
  }


  @GET
  @Path("{groupId}/{packageName}/{version}/{buildNumber}/pom.xml")
  @Produces("application/xml")
  public String getPom(@PathParam("groupId") String groupId,
                       @PathParam("packageName") String packageName,
                       @PathParam("version") String version,
                       @PathParam("buildNumber") String buildNumber) throws IOException {

    PackageVersionId packageVersionId = new PackageVersionId(groupId, packageName, version);
    PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

    PackageStatus packageStatus = PackageDatabase.getStatus(packageVersionId, RenjinVersionId.RELEASE);

    PackageBuild build = new PackageBuild(packageVersion.getPackageVersionId(), 100);
    build.setRenjinVersion(RenjinVersionId.RELEASE.toString());
    build.setDependencies(packageStatus.getDependencies());

    PomBuilder pomBuilder = new PomBuilder(build, packageVersion.parseDescription());
    return pomBuilder.getXml();
  }
}
