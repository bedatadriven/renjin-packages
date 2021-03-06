
package org.renjin.ci.packages;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.objectify.*;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.NoRobots;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.TestCase;
import org.renjin.ci.repo.apt.AptArtifact;
import org.renjin.ci.storage.StorageKeys;

import javax.annotation.Nonnull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specific version of a package
 */
@NoRobots
public class PackageVersionResource {
  
  private static final Logger LOGGER = Logger.getLogger(PackageVersionResource.class.getName());
  
  private final PackageVersion packageVersion;
  private PackageVersionId packageVersionId;

  public PackageVersionResource(PackageVersion packageVersion) {
    this.packageVersionId = packageVersion.getPackageVersionId();
    this.packageVersion = packageVersion;
  }

  @GET
  @Produces("text/html")
  public Viewable getPage() {
    return getPage(false);
  }


  /**
   *
   * @param index true if this page may be indexed by robots
   * @return
   */
  Viewable getPage(boolean index) {
    PackageVersionPage viewModel = new PackageVersionPage(packageVersion);
  
    Map<String, Object> model = new HashMap<>();
    model.put("version", viewModel);
    model.put("index", index);

    return new Viewable("/packageVersion.ftl", model);
  }

  @Path("build/{buildNumber}")
  public PackageBuildResource getBuild(@PathParam("buildNumber") int buildNumber) {
    return new PackageBuildResource(packageVersion.getPackageVersionId(), buildNumber);
  }

  @GET
  @Path("shield")
  @Produces("image/svg+xml")
  public Viewable getShield() {

    Map<String, Object> model = new HashMap<>();
    model.put("status", "n/a");
    model.put("statusColor", "gray");

    if(packageVersion.hasBuild()) {

      PackageBuild build = PackageDatabase.getBuild(packageVersion.getLastBuildId()).now();
      switch (build.getGrade()) {
        case "A":
          model.put("status", "ok");
          model.put("statusColor", "#87b13f");
          break;
        case "B":
        case "C":
          model.put("status", "warnings");
          model.put("statusColor", "#dd8822");
          break;
        default:
        case "D":
        case "F":
          model.put("status", "errors");
          model.put("statusColor", "#e05d44");
          break;
      }
    }

    return new Viewable("/shield.ftl", model);
  }
  
  @GET
  @Path("lastSuccessfulBuild")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLastSuccessfulBuild() {
    if(packageVersion.hasSuccessfulBuild()) {
      return Response.ok().entity(packageVersion.getLastSuccessfulBuildVersion()).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Allocate a new build number for this package version
   */
  @POST
  @Path("builds")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  @NoRobots
  public PackageBuild startBuild(@FormParam("renjinVersion") final String renjinVersion) {
    
    if(Strings.isNullOrEmpty(renjinVersion)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    return ObjectifyService.ofy().transactNew(new Work<PackageBuild>() {
      @Override
      public PackageBuild run() {
        PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();

        // increment next build number
        long nextBuild = packageVersion.getLastBuildNumber() + 1;
        packageVersion.setLastBuildNumber(nextBuild);

        PackageBuild packageBuild = new PackageBuild(packageVersionId, nextBuild);
        packageBuild.setRenjinVersion(renjinVersion);
        packageBuild.setStartTime(new Date().getTime());

        ObjectifyService.ofy().save().entities(packageBuild, packageVersion);

        return packageBuild;
      }
    });
  }

  @GET
  @Path("examples")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TestCase> getExamples() {
    List<PackageExample> examples = ObjectifyService.ofy().load().type(PackageExample.class)
        .ancestor(PackageVersion.key(packageVersionId))
        .chunk(1000)
        .list();

    if(examples.isEmpty()) {
      examples = ExamplesExtractor.extract(packageVersionId);
    }
    
    List<Key<PackageExampleSource>> sources = new ArrayList<>();
    for (PackageExample example : examples) {
      if(example.getSource() != null) {
        sources.add(example.getSource().getKey());
      }
    }

    Map<Key<PackageExampleSource>, PackageExampleSource> sourceMap = ObjectifyService.ofy().load().keys(sources);
    
    List<TestCase> cases = new ArrayList<>();
    for (PackageExample example : examples) {
      if(example.getSource() != null) {
        PackageExampleSource source = sourceMap.get(example.getSource().getKey());
        if (source != null && !Strings.isNullOrEmpty(source.getSource())) {
          TestCase testCase = new TestCase();
          testCase.setId(example.getName());
          testCase.setSource(source.getSource());
          cases.add(testCase);
        }
      }
    }
    
    return cases;
  }
  
  @GET
  @Path("builds")
  @NoRobots
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageBuild> getBuilds() {
    return Lists.newArrayList(PackageDatabase.getBuilds(packageVersionId));
  }
  
  @POST
  @Path("updateDeltas")
  @Produces(MediaType.TEXT_PLAIN)
  public String updateDeltas() {
    PackageDatabase.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        DeltaBuilder.update(packageVersionId);
      }
    });
    return "Done.";
  }

  @Path("resolveDependencies")
  public DependencyResolution resolveDependencies() {
    try {
      return new DependencyResolution(packageVersion);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception thrown while resolving dependencies: " + e.getMessage(), e);
      throw e;
    }
  }

  @GET
  @Produces("text/html")
  @Path("source")
  @NoRobots
  public Viewable getSourceIndex() {
    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("files", Lists.newArrayList(PackageDatabase.getPackageSourceKeys(packageVersionId)));
    
    return new Viewable("/packageSourceIndex.ftl", model);
  }

  @GET
  @Produces("text/html")
  @Path("source/{file:.+}")
  @NoRobots
  public Viewable getSourceFile(@PathParam("file") String filename) {

    LoadResult<PackageSource> source = PackageDatabase.getSource(packageVersionId, filename);

    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("filename", filename);
    model.put("lines", source.safe().parseLines());
    
    return new Viewable("/packageSource.ftl", model);
  }
  
  @GET
  @Produces("text/html")
  @Path("buildDependencyMatrix")
  @NoRobots
  public Viewable compareDependencies(@QueryParam("test") String testName) {

    Map<String, Object> model = new HashMap<>();
    model.put("page", new DepMatrixPage(packageVersion, Optional.fromNullable(testName)));

    return new Viewable("/buildDepMatrix.ftl", model);
  }
  
  @GET
  @Produces("text/html")
  @Path("test/{testName}/history")
  @NoRobots
  public Viewable getTestHistory(@PathParam("testName") String testName, @QueryParam("pull") Long pullNumber) {
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", new TestHistoryPage(packageVersion, testName, pullNumber));
    
    return new Viewable("/testHistory.ftl", model);
  }

  @GET
  @Produces("text/csv")
  @Path("test/{testName}/timings.csv")
  @NoRobots
  public String getTestTimings(@PathParam("testName") String testName) {

    StringBuilder csv = new StringBuilder();

    // Headers
    csv.append("interpreter,version,jdk,blas,time,passed\n");

    // Write timings as rows
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersion.getPackageVersionId(), testName);
    for (PackageTestResult result : results) {
      if(result.getDuration() != 0) {
        csv.append("Renjin,");
        csv.append(result.getRenjinVersion());
        csv.append(",Unknown,f2jblas,");
        csv.append(result.getDuration());
        if(result.isPassed()) {
          csv.append(",true");
        } else {
          csv.append(",false");
        }
        csv.append("\n");
      }
    }

    return csv.toString();
  }

  @GET
  @Produces("application/gzip")
  @Path("source.tar.gz")
  @NoRobots
  public Response getSourceArchive() {
    BlobKey blobKey = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(
      "/gs/" + StorageKeys.PACKAGE_SOURCE_BUCKET + "/" + StorageKeys.packageSource(packageVersionId));

    return Response.ok()
      .header("X-AppEngine-BlobKey", blobKey.getKeyString())
      .build();
  }
}
