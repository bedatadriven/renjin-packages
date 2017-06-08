package org.renjin.ci.source.index;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Provides redirects to R source code
 */
public class RRedirects {

  private static final Logger LOGGER = Logger.getLogger(RRedirects.class.getName());

  @GET
  public Response redirectToFunctionDeclaration(@Context UriInfo uriInfo,
                                                @QueryParam("function") String functionName,
                                                @QueryParam("build") String buildId) {


    Optional<Key<PackageSource>> packageSource = findPackageSource(functionName, buildId);
    if(packageSource.isPresent()) {

      URI sourceUri = uriInfo.getBaseUriBuilder().path(PackageSource.getPath(packageSource.get())).build();

      return Response.temporaryRedirect(sourceUri).build();
    }

    return Response.status(Response.Status.NOT_FOUND).entity("Couldn't resolve the function reference :-(").build();
  }

  private Optional<Key<PackageSource>> findPackageSource(String functionName, String buildId) {

    LoadResult<PackageBuild> buildResult = PackageDatabase.getBuild(PackageBuildId.fromString(buildId));

    LOGGER.info("Searching Function Index for " + functionName);

    QueryResultIterable<Key<FunctionIndex>> sources = ObjectifyService.ofy()
        .load()
        .type(FunctionIndex.class)
        .chunk(100)
        .filter("def", functionName)
        .keys()
        .iterable();

    // Build a map from all definitions to their package versions
    Multimap<PackageVersionId, Key<PackageSource>> index = HashMultimap.create();
    for (Key<FunctionIndex> source : sources) {
      Key<PackageSource> packageSourceKey = FunctionIndex.packageSourceKey(source);
      Key<PackageVersion> packageVersionKey = packageSourceKey.getParent();
      PackageVersionId packageVersionId = PackageVersion.idOf(packageVersionKey);

      LOGGER.info("Defined in " + packageSourceKey);

      index.put(packageVersionId, packageSourceKey);
    }

    PackageBuild build = buildResult.safe();

    // Is this function defined in the package being built?
    if(index.containsKey(build.getPackageVersionId())) {
      return Optional.of(index.get(build.getPackageVersionId()).iterator().next());
    }

    // Otherwise check dependencies...
    for (PackageVersionId packageVersionId : build.getResolvedDependencyIds()) {
      if(index.containsKey(packageVersionId)) {
        return Optional.of(index.get(packageVersionId).iterator().next());
      }
    }

    return Optional.absent();
  }

}
