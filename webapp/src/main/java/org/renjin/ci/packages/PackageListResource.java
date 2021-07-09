package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.search.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.*;
import org.renjin.ci.index.PackageSearchIndex;
import org.renjin.ci.model.*;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/packages")
public class PackageListResource {

  private static final Logger LOGGER = Logger.getLogger(PackageListResource.class.getName());

  private static final int MAX_RESULTS = 50;

  /**
   * Cache package index page 10 hours.
   */
  private static final int INDEX_CACHE_SECONDS = 10 * 60 * 60;

  private final SearchService searchService = SearchServiceFactory.getSearchService();

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response getIndex() {
    return getIndex("A");
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("{letter:[A-Z]}")
  public Response getIndex(@PathParam("letter") String letter) {

    Map<String, Object> model = new HashMap<>();
    model.put("letters", Lists.charactersOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    model.put("packages", getPackagesStartingWithLetter(letter.charAt(0)));

    CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(INDEX_CACHE_SECONDS);
    cacheControl.setPrivate(false);

    return
        Response.ok(new Viewable("/packageIndex.ftl", model))
        .cacheControl(cacheControl)
        .expires(new Date(System.currentTimeMillis() + (INDEX_CACHE_SECONDS * 1000)))
        .build();
  }

  private List<Package> getPackagesStartingWithLetter(char letter) {

    MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
    String memcacheKey = "packages-" + letter;

    List<Package> list = null;
    try {
      list = (List<Package>) memcacheService.get(memcacheKey);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception retrieving package list " + memcacheKey + " from memcache", e);
    }

    if(list == null) {
      list = PackageDatabase.getPackagesStartingWithLetter(letter);
      memcacheService.put(memcacheKey, list, Expiration.byDeltaSeconds(INDEX_CACHE_SECONDS));
    }

    return list;
  }

  @GET
  @Path("/all")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getAll() {
    List<PackageVersionId> packageVersionIds = new ArrayList<>();
    QueryResultIterable<Key<PackageVersion>> packages = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .chunk(1000)
        .keys()
        .iterable();

    for (Key<PackageVersion> packageVersionKey : packages) {
      packageVersionIds.add(PackageVersion.idOf(packageVersionKey));
    }
    return packageVersionIds;
  }

  @GET
  @Path("/unbuilt")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getUnbuilt() {
    List<PackageVersionId> results = new ArrayList<>();
    QueryResultIterable<Package> packages = ObjectifyService.ofy()
        .load()
        .type(Package.class)
        .chunk(3000)
        .iterable();

    for (Package aPackage : packages) {
      if(!aPackage.isReplaced() && aPackage.getLatestVersion() != null &&
          aPackage.getGrade() == null) {
        results.add(aPackage.getLatestVersionId());
      }
    }
    return results;
  }

  @GET
  @Path("/built")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getBuilt() {
    List<PackageVersionId> packageVersionIds = new ArrayList<>();
    QueryResultIterable<PackageVersion> versions = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .chunk(1000)
        .iterable();

    for (PackageVersion version : versions) {
      if(version.getLastBuildNumber() > 0) {
        packageVersionIds.add(version.getPackageVersionId());
      }
    }

    return packageVersionIds;
  }

  @GET
  @Path("/new")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getNew() {
    List<PackageVersionId> packageVersionIds = new ArrayList<>();
    QueryResultIterable<PackageVersion> packages = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .order("-publicationDate")
        .chunk(1000)
        .iterable();

    DateTime cutoff = new DateTime().minusDays(60);

    for (PackageVersion packageVersion : packages) {
      if(packageVersion.getLastBuildNumber() == 0) {

        // Check if this package is replaced:
        Package pkg = PackageDatabase.getPackageOf(packageVersion.getPackageVersionId());
        if(!pkg.isReplaced()) {
          packageVersionIds.add(packageVersion.getPackageVersionId());
        }
      }
      if(cutoff.isAfter(packageVersion.getPublicationDate().getTime())) {
        break;
      }
    }
    return packageVersionIds;
  }

  @GET
  @Path("/regressions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getPackagesWithRegressions() {
    return queryRegressions("regression");
  }

  @GET
  @Path("/buildRegressions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getPackagesWithBuildRegressions() {
    return queryRegressions("buildRegression");
  }

  @GET
  @Path("/compilationRegressions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getPackagesWithCompilationRegressions() {
    return queryRegressions("compilationRegression");
  }

  private List<PackageVersionId> queryRegressions(String filter) {
    QueryResultIterable<Key<PackageVersionDelta>> deltas = ObjectifyService.ofy()
        .load()
        .type(PackageVersionDelta.class)
        .filter(filter, true)
        .chunk(200)
        .keys()
        .iterable();


    List<PackageVersionId> packageVersions = new ArrayList<>();
    for (Key<PackageVersionDelta> deltaKey : deltas) {
      packageVersions.add(PackageVersionId.fromTriplet(deltaKey.getName()));
    }

    return packageVersions;
  }


  @GET
  @Path("/needsCompilation")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<PackageVersionId> getNeedingCompilation() {
    QueryResultIterable<Key<PackageVersion>> packages = ObjectifyService.ofy()
        .load()
        .type(PackageVersion.class)
        .filter("needsCompilation", true)
        .chunk(1000)
        .keys()
        .iterable();

    Map<PackageId, PackageVersionId> packageVersionIds = new HashMap<>();

    for (Key<PackageVersion> packageVersionKey : packages) {
      PackageVersionId newPvid = PackageVersion.idOf(packageVersionKey);
      PackageVersionId existingPvid = packageVersionIds.get(newPvid.getPackageId());
      if(existingPvid == null || newPvid.isNewer(existingPvid)) {
        packageVersionIds.put(newPvid.getPackageId(), newPvid);
      }
    }
    return packageVersionIds.values();
  }

  @GET
  @Path("/failing")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getFailing() {
    List<PackageVersionId> results = new ArrayList<>();
    QueryResultIterable<Package> packages = ObjectifyService.ofy()
        .load()
        .type(Package.class)
        .chunk(3000)
        .iterable();

    for (Package aPackage : packages) {
      if(!aPackage.isReplaced() &&
          aPackage.getLatestVersion() != null &&
          aPackage.getGradeInteger() == PackageBuild.GRADE_F) {
        results.add(aPackage.getLatestVersionId());
      }
    }
    return results;
  }

  @GET
  @Path("/passing")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getPassing() {
    List<PackageVersionId> results = new ArrayList<>();
    QueryResultIterable<Package> packages = ObjectifyService.ofy()
        .load()
        .type(Package.class)
        .chunk(3000)
        .iterable();

    for (Package aPackage : packages) {
      if(!aPackage.isReplaced() &&
          aPackage.getLatestVersion() != null &&
          aPackage.getGradeInteger() == PackageBuild.GRADE_A) {
        results.add(aPackage.getLatestVersionId());
      }
    }
    return results;
  }

  @GET
  @Path("/latest")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageVersionId> getLatest() {
    List<PackageVersionId> results = new ArrayList<>();
    QueryResultIterable<Package> packages = ObjectifyService.ofy()
        .load()
        .type(Package.class)
        .chunk(3000)
        .iterable();

    for (Package aPackage : packages) {
      if(!aPackage.isReplaced() && aPackage.getLatestVersion() != null) {
        results.add(aPackage.getLatestVersionId());
      }
    }
    return results;
  }

  @GET
  @Path("/{name}.html")
  public Response getPackageLocation(@Context UriInfo uriInfo, @PathParam("name") String packageName) {
    return Response
        .status(Response.Status.MOVED_PERMANENTLY)
        .location(uriInfo.getBaseUriBuilder().path("package").path("org.renjin.cran").path(packageName).build())
        .build();
  }
  @GET
  @Path("/contribute")
  public Viewable getContributionForm() {
    return new Viewable("/contribute.ftl", Collections.emptyMap());
  }

  @POST
  @Path("/contribute")
  public Response submitContribution(@Context UriInfo uriInfo,
                                     @FormParam("groupId") String groupId,
                                     @FormParam("artifactId") String artifactId,
                                     @FormParam("latestVersion") String latestVersion,
                                     @FormParam("projectUrl") String projectUrl,
                                     @FormParam("title") String title) {


    PackageVersionId pvid = new PackageVersionId(groupId, artifactId, latestVersion);
    Contribution contribution = Contribution.fetchFromMaven(pvid);
    if(contribution == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Could not find artifact in maven central").build());
    }

    Package existingPackage = ObjectifyService.ofy().load().key(Package.key(pvid.getPackageId())).now();

    if(existingPackage != null && !existingPackage.isContributed()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
              .entity("Package " + existingPackage.getPackageId() + " exists but is not contributed.").build());
    }
    if(existingPackage == null) {
      existingPackage = new Package(pvid.getPackageId());
      existingPackage.setName(pvid.getPackageName());
      existingPackage.setContributed(true);
      existingPackage.setLatestVersion(pvid.getVersion());
    }
    existingPackage.setProjectUrl(contribution.getUrl());
    existingPackage.setTitle(contribution.getDescription());
    if(pvid.compareTo(existingPackage.getLatestVersionId()) > 0) {
      existingPackage.setLatestVersion(pvid.getVersion());
    }
    ObjectifyService.ofy().save().entity(existingPackage).now();

    URI packageUri = uriInfo.getAbsolutePathBuilder()
            .replacePath(pvid.getPackageId().getPath())
            .build();

    return Response.seeOther(packageUri).build();
  }

  @GET
  @Path("/resolveDependencySnapshot")
  @Produces(MediaType.APPLICATION_JSON)
  public ResolvedDependencySet resolveDependenciesGet(@QueryParam("beforeDate") String beforeDate, @QueryParam("p") List<String> packageNames) {
    DependencySnapshotRequest request = new DependencySnapshotRequest();
    request.setBeforeDate(beforeDate);
    request.getDependencies().addAll(packageNames);
    return resolveDependencies(request);
  }

  @GET
  @Path("/resolveDependencySnapshotScript")
  @Produces(MediaType.TEXT_PLAIN)
  public String resolveDependenciesScript(@QueryParam("beforeDate") String beforeDate, @QueryParam("p") List<String> packageNames) {
    ResolvedDependencySet set = resolveDependenciesGet(beforeDate, packageNames);
    StringBuilder script = new StringBuilder();
    for (ResolvedDependency resolvedDependency : set.getDependencies()) {
      if(resolvedDependency.isVersionResolved()) {
        script.append(String.format("install.packages(\"%s\", repos = NULL)\n",
            StorageKeys.packageSourceUrl(resolvedDependency.getPackageVersionId())));
      } else {
        script.append(String.format("# Unresolved: %s\n", resolvedDependency.getName()));
      }
    }

    return script.toString();
  }

  @GET
  @Path("/resolveSuggests")
  @Produces(MediaType.APPLICATION_JSON)
  public ResolvedDependencySet resolvedSuggests(@QueryParam("p") List<String> packageNames) {
    SuggestsResolution suggestsResolution = new SuggestsResolution();
    return suggestsResolution.resolve(packageNames);
  }

  @GET
  @Path("/resolve")
  @Produces(MediaType.APPLICATION_JSON)
  public String resolve(@QueryParam("p") List<String> dependencies) throws JSONException {
    SuggestsResolution suggestsResolution = new SuggestsResolution();
    ResolvedDependencySet set = suggestsResolution.resolve(dependencies);

    JSONArray array = new JSONArray();
    for (ResolvedDependency resolvedDependency : set.getDependencies()) {
      JSONObject dependency = new JSONObject();
      dependency.put("package", resolvedDependency.getName());
      if(resolvedDependency.isVersionResolved()) {
        dependency.put("resolved", true);
        dependency.put("groupId", resolvedDependency.getPackageVersionId().getGroupId());
        if(resolvedDependency.isReplaced()) {
          dependency.put("version", resolvedDependency.getReplacementVersion());
        } else {
          if(resolvedDependency.hasBuild()) {
            dependency.put("version", resolvedDependency.getBuildId().getBuildVersion());
          }
        }
      }
      array.put(dependency);
    }
    return array.toString();
  }

  @POST
  @Path("/resolveDependencySnapshot")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResolvedDependencySet resolveDependencies(DependencySnapshotRequest request) {

    try {
      SnapshotResolution resolution = new SnapshotResolution(LocalDate.parse(request.getBeforeDate()));
      for (String packageName : request.getDependencies()) {
        resolution.addPackage(packageName);
      }
      return resolution.build();
    } catch (Exception e) {
      throw new WebApplicationException(Response
          .status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(e.getMessage())
          .build());
    }
  }

  @GET
  @Path("/resolveDependencies")
  public Response resolveDependencies(@Context UriInfo uriInfo) throws JSONException {

    MultivaluedMap<String, String> packages = uriInfo.getQueryParameters(true);

    // Map the package names to FQNs
    List<Package> packageList = ObjectifyService.ofy().load()
        .type(Package.class)
        .filter("name in", packages.keySet())
        .list();
    Map<String, PackageId> map = new HashMap<>();
    for (Package aPackage : packageList) {
      map.put(aPackage.getName(), aPackage.getPackageId());
    }

    // Now query package versions
    List<PackageVersionId> roots = new ArrayList<>();
    for (String rootPackage : packages.keySet()) {
      PackageId rootPackageId = map.get(rootPackage);
      String version = packages.getFirst(rootPackage);
      if(rootPackageId == null) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
            .entity("Could not find package named '" + rootPackage + "'").build());
      }
      roots.add(new PackageVersionId(rootPackageId, version));
    }

    DependencyResolutionMulti resolver = new DependencyResolutionMulti(roots);
    List<PackageVersionId> dependencies = resolver.resolve();

    JSONArray array = new JSONArray();
    int arrayIndex = 0;
    for (PackageVersionId packageVersionId : dependencies) {
      array.put(arrayIndex++, packageVersionId.toString());
    }
    return Response.ok().entity(array.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
  }

  @GET
  @Path("search")
  @Produces("text/html")
  public Viewable search(@QueryParam("q") String queryString) {
    Map<String, Object> model = new HashMap<>();
    model.put("queryString", queryString);

    if(Strings.isNullOrEmpty(queryString)) {
      model.put("resultCount", 0);
      model.put("pageTitle", "Package Search");
      model.put("results", Collections.emptyList());
    } else {

      QueryResultIterable<Package> exactMatches = ObjectifyService.ofy()
              .load()
              .type(Package.class)
              .filter("name", queryString)
              .iterable();

      Index index = searchService.getIndex(IndexSpec.newBuilder().setName(PackageSearchIndex.INDEX_NAME));
      Query query = Query.newBuilder()
          .setOptions(QueryOptions.newBuilder()
              .setFieldsToReturn(
                  PackageSearchIndex.TITLE_FIELD,
                  PackageSearchIndex.DESCRIPTION_FIELD)
              .setLimit(MAX_RESULTS))
          .build(queryString);

      Results<ScoredDocument> results = index.search(query);

      List<SearchResult> resultList = new ArrayList<>();
      for (Package exactMatch : exactMatches) {
        resultList.add(new SearchResult(exactMatch));
      }

      for (ScoredDocument scoredDocument : results.getResults()) {
        resultList.add(new SearchResult(scoredDocument));
      }

      model.put("pageTitle", String.format("Package Search Results for '%s'", queryString));
      model.put("results", resultList);
      model.put("resultCount", results.getNumberFound());
    }

    return new Viewable("/packageSearch.ftl", model);
  }

  public static class SearchResult {
    private final String groupId;
    private final String packageName;
    private final String title;
    private final String description;

    public SearchResult(ScoredDocument document) {
      Preconditions.checkNotNull(document);
      String[] packageId = document.getId().split(":");
      this.groupId = packageId[0];
      this.packageName = packageId[1];
      this.title = document.getOnlyField(PackageSearchIndex.TITLE_FIELD).getText();
      if(document.getFieldCount(PackageSearchIndex.DESCRIPTION_FIELD)  > 0) {
        this.description = document.getFields(PackageSearchIndex.DESCRIPTION_FIELD).iterator().next().getText();
      } else {
        this.description = null;
      }
    }

    public SearchResult(Package matchingPackage) {
      this.groupId = matchingPackage.getGroupId();
      this.packageName = matchingPackage.getName();
      this.title = matchingPackage.getTitle();
      this.description = null;
    }

    public String getGroupId() {
      return groupId;
    }

    public String getPackageName() {
      return packageName;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getUrl() {
      return "/package/" + getGroupId() +  "/" + getPackageName();
    }
  }


}
