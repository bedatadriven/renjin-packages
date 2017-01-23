package org.renjin.ci.releases;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.googlecode.objectify.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinCommit;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.model.RenjinVersionId;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Path("/releases")
public class ReleasesResource {




  @GET
  @Path("latest")
  @Produces(MediaType.TEXT_PLAIN)
  public String getLatest() {
    return PackageDatabase.getLatestRelease().toString();
  }
  
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response postNewRelease(@FormParam("renjinVersion") final String renjinVersion, 
                                 @FormParam("buildNumber") final long buildNumber,
                                 @FormParam("sha1") final String commitId) {

    return ObjectifyService.ofy().transact(new Work<Response>() {
      @Override
      public Response run() {

        Key<RenjinRelease> releaseKey = Key.create(RenjinRelease.class, renjinVersion);
        RenjinRelease release = ObjectifyService.ofy().load().key(releaseKey).now();
        if (release != null) {
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("Release exists already")
              .build();
        }

        release = new RenjinRelease();
        release.setVersion(renjinVersion);
        release.setBuildNumber(buildNumber);
        release.setDate(new Date());
        release.setRenjinCommit(Ref.create(Key.create(RenjinCommit.class, commitId)));

        ObjectifyService.ofy().save().entity(release);

        return Response.ok().build();
      }
    });
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersionRange(@QueryParam("from") final String from, @QueryParam("to") final String to) throws JSONException {

    Predicate<RenjinVersionId> predicate = Predicates.alwaysTrue();
    if(!Strings.isNullOrEmpty(from)) {
      final RenjinVersionId fromVersion = RenjinVersionId.valueOf(from);
      predicate = new Predicate<RenjinVersionId>() {

        @Override
        public boolean apply(@Nullable RenjinVersionId input) {
          return input.compareTo(fromVersion) >= 0;
        }
      };
    }
    if(!Strings.isNullOrEmpty(to)) {
      final RenjinVersionId toVersion = RenjinVersionId.valueOf(to);
      predicate = Predicates.and(predicate, new Predicate<RenjinVersionId>() {
        @Override
        public boolean apply(@Nullable RenjinVersionId input) {
          return input.compareTo(toVersion) <= 0;
        }
      });
    }

    JSONArray array = new JSONArray();
    int arrayIndex = 0;
    
    DateFormat format = new SimpleDateFormat("YYYY-MM-dd");
    
    
    for (RenjinRelease renjinRelease : PackageDatabase.getRenjinVersions()) {
      if(predicate.apply(renjinRelease.getVersionId())) {
        JSONObject object = new JSONObject();
        object.put("version", renjinRelease.getVersion());
        object.put("sha1", renjinRelease.getCommitSha1());
        object.put("date", format.format(renjinRelease.getDate()));
        array.put(arrayIndex++, object);
      }
    }
    
    return Response.ok().entity(array.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @GET
  @Path("compare")
  public Response compareRenjinVersions(@QueryParam("from") String fromVersion, @QueryParam("to") String toVersion) 
      throws URISyntaxException {
    
    RenjinVersionId from = RenjinVersionId.valueOf(fromVersion);
    RenjinVersionId to = RenjinVersionId.valueOf(toVersion);

    LoadResult<RenjinRelease> fromRelease = PackageDatabase.getRenjinRelease(from);
    LoadResult<RenjinRelease> toRelease = PackageDatabase.getRenjinRelease(to);
    
    if(fromRelease.now() == null || toRelease.now() == null) {
      return Response.status(503).entity("Not available").build();
    }

    return Response.temporaryRedirect(
        new URI("https://github.com/bedatadriven/renjin/compare/" + 
            fromRelease.now().getCommitSha1() + "..." + toRelease.now().getCommitSha1())).build();
  }

  public static String compareUrl(RenjinVersionId from, RenjinVersionId to) {
    return "/releases/compare?from=" + from + "&to=" + to;
  }
}
