package org.renjin.ci.admin;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.mapreduce.*;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.GoogleCloudStorageFileOutput;
import com.google.appengine.tools.mapreduce.reducers.ValueProjectionReducer;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import com.google.common.base.Optional;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.admin.migrate.*;
import org.renjin.ci.archive.ExamplesExtractor;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.index.GitHubTasks;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachEntity;
import org.renjin.ci.pipelines.ForEachPackageVersion;
import org.renjin.ci.pipelines.Pipelines;
import org.renjin.ci.source.index.LocCounter;
import org.renjin.ci.source.index.UpdateLocStats;
import org.renjin.ci.stats.StatPipelines;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.status;

@Path("/admin")
public class AdminResources {


  @GET
  @Produces(MediaType.TEXT_HTML)
  public Viewable get() {
    
    Map<String, Object> model = new HashMap<>();
    model.put("migrations", Arrays.asList(
        ReComputeBuildDeltas.class,
        ReIndexPackageVersion.class,
        UpdatePubDates.class,
        ReIndexPackage.class,
        MigrateTestOutput.class,
        ReindexTestResults.class,
        RecomputeBuildGrades.class,
        PackagesWithoutNamespaces.class,
        LocCounter.class));
    
    return new Viewable("/admin.ftl", model);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("/disable")
  public Viewable getDisablePage(@QueryParam("packageVersionId") String packageVersion) {
    PackageVersionId pvid = PackageVersionId.fromTriplet(packageVersion);
   
    Map<String, Object> model = new HashMap<>();
    model.put("packageVersionId", pvid.toString());
    
    return new Viewable("/disablePackageVersion.ftl", model);
  }
  
  @POST
  @Path("rebuildExamples")
  public Response rebuildExamples() {
    return Pipelines.redirectToStatus(Pipelines.forEach(new ExamplesExtractor()));
  }

  @POST
  @Path("migrate")
  public Response migrateEntity(@FormParam("functorClass") String functorClassName) {

    Class<?> functorClass;
    try {
      functorClass = Class.forName(functorClassName);
    } catch (ClassNotFoundException e) {
      throw new WebApplicationException(
           status(Response.Status.BAD_REQUEST)
          .entity("Class not found: " + functorClassName)
          .build());
    }

    Object functor;
    try {
      functor = functorClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    String jobId;
    if(functor instanceof ForEachEntity) {
      jobId = Pipelines.applyAll((ForEachEntity) functor);
    } else if(functor instanceof ForEachPackageVersion) {
      jobId = Pipelines.forEach((ForEachPackageVersion) functor);
    } else {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
      .entity("Unsupported functor class: " + functor.getClass().getName()).build());
    }
    
    return Pipelines.redirectToStatus(jobId);
  }

  @POST
  @Path("updateDeltaCounts")
  public Response updateBuildDeltaCounts() {
    return Pipelines.redirectToStatus(StatPipelines.startUpdateBuildStats());
  }

  @POST
  @Path("/queueUpdateTotalCounts")
  public Response queueUpdateTotalCounts() {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/admin/updateTotalCounts"));
    return Response.ok().entity("Enqueued.").build();
  }

  @POST
  @Path("/updateTotalCounts")
  public Response updateTotalCounts() {
    UpdateLocStats stats = new UpdateLocStats();
    stats.run();

    return Response.ok().build();
  }

  @POST
  @Path("/exportBuilds")
  public Response exportBuilds() {
    DatastoreInput input = new DatastoreInput("PackageBuild", 10);
    BuildExporter mapper = new BuildExporter();
    Reducer<String, ByteBuffer, ByteBuffer> reducer = new ValueProjectionReducer<>();
    GoogleCloudStorageFileOutput output = new GoogleCloudStorageFileOutput("renjinci-exports", "builds-%d.csv", "text/csv");

    MapReduceSpecification<Entity, String, ByteBuffer, ByteBuffer, GoogleCloudStorageFileSet>
            spec = new MapReduceSpecification.Builder<>(input, mapper, reducer, output)
            .setKeyMarshaller(Marshallers.getStringMarshaller())
            .setValueMarshaller(Marshallers.getByteBufferMarshaller())
            .setJobName("Export Build List")
            .setNumReducers(10)
            .build();

    MapReduceSettings settings = new MapReduceSettings.Builder()
            .setBucketName("renjinci-map-reduce")
            .build();

    MapReduceJob<Entity, String, ByteBuffer, ByteBuffer, GoogleCloudStorageFileSet> job = new MapReduceJob<>(spec, settings);
    String jobId = PipelineServiceFactory.newPipelineService().startNewPipeline(job);

    return Pipelines.redirectToStatus(jobId);
  }

  @POST
  @Path("/exportPackageVersions")
  public Response exportPackageVersions() {
    DatastoreInput input = new DatastoreInput("PackageVersion", 10);
    PackageVersionExporter mapper = new PackageVersionExporter();
    Reducer<String, ByteBuffer, ByteBuffer> reducer = new ValueProjectionReducer<>();
    GoogleCloudStorageFileOutput output = new GoogleCloudStorageFileOutput("renjinci-exports", "package-versions-%d.csv", "text/csv");

    MapReduceSpecification<Entity, String, ByteBuffer, ByteBuffer, GoogleCloudStorageFileSet>
        spec = new MapReduceSpecification.Builder<>(input, mapper, reducer, output)
        .setKeyMarshaller(Marshallers.getStringMarshaller())
        .setValueMarshaller(Marshallers.getByteBufferMarshaller())
        .setJobName("Export Package Versions List")
        .setNumReducers(10)
        .build();

    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    MapReduceJob<Entity, String, ByteBuffer, ByteBuffer, GoogleCloudStorageFileSet> job = new MapReduceJob<>(spec, settings);
    String jobId = PipelineServiceFactory.newPipelineService().startNewPipeline(job);

    return Pipelines.redirectToStatus(jobId);
  }

  @POST
  @Path("addGitHubRepo")
  public Response addGitHubRepo(@FormParam("repo") String repoId) {
    
    String[] repoParts = repoId.split("/");
    if(repoParts.length != 2) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Malformed github repo name").build();
    }

    Optional<Package> cranPackage = PackageDatabase.getPackageIfExists(new PackageId("org.renjin.cran", repoParts[1]));
    if(cranPackage.isPresent()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("CRAN package with name '" + repoParts[1] + "' already exists.").build();
    }
    
    PackageId packageId = new PackageId("org.renjin.github." + repoParts[0], repoParts[1]);
    
    Optional<Package> gitHubPackage = PackageDatabase.getPackageIfExists(packageId);
    if(gitHubPackage.isPresent()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Package " + packageId + " already exists.").build();
    }

    GitHubTasks.enqueue(repoParts[0], repoParts[1]);
    
    return Response.ok().entity("Enqueued.").build();
    
  }
}
