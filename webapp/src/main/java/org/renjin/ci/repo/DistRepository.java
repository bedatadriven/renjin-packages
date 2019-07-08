package org.renjin.ci.repo;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("repo/dist")
public class DistRepository {

    @GET
    @Path("renjin-{version}.zip")
    public Response getZipArchive(@PathParam("version") String version) {
        BlobKey blobKey = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(
            "/gs/" + StorageKeys.REPO_BUCKET + "/dist/renjin-" + version + ".zip");

        return Response.ok()
            .header("X-AppEngine-BlobKey", blobKey.getKeyString())
            .header("Cache-Control", "max-age=365000000, immutable")
            .build();
    }

}
