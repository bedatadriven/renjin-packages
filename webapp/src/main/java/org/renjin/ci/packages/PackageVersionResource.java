
package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.ReComputeBuildDeltas;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.TestCase;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specific version of a package
 */
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
    PackageVersionPage viewModel = new PackageVersionPage(packageVersion);
  
    Map<String, Object> model = new HashMap<>();
    model.put("version", viewModel);

    return new Viewable("/packageVersion.ftl", model);
  }

  @Path("build/{buildNumber}")
  public PackageBuildResource getBuild(@PathParam("buildNumber") int buildNumber) {
    return new PackageBuildResource(packageVersion.getPackageVersionId(), buildNumber);
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
  @Produces(MediaType.APPLICATION_JSON)
  public List<PackageBuild> getBuilds() {
    return Lists.newArrayList(PackageDatabase.getBuilds(packageVersionId));
  }
  
  @GET
  @Path("check")
  public Response check() {
    ReComputeBuildDeltas markBuildDeltas = new ReComputeBuildDeltas();
    markBuildDeltas.map(PackageVersion.key(packageVersionId).getRaw());

    return Response.ok("Done").build();
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
  public Viewable getSourceIndex() {
    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("files", Lists.newArrayList(PackageDatabase.getPackageSourceKeys(packageVersionId)));
    
    return new Viewable("/packageSourceIndex.ftl", model);
  }

  @GET
  @Produces("text/html")
  @Path("source/{file:.+}")
  public Viewable getSourceFile(@PathParam("file") String filename) {

    LoadResult<PackageSource> source = PackageDatabase.getSource(packageVersionId, filename);

    Map<String, Object> model = new HashMap<>();
    model.put("version", packageVersion);
    model.put("filename", filename);
    model.put("lines", source.safe().parseLines());
    
    return new Viewable("/packageSource.ftl", model);
  }
}
