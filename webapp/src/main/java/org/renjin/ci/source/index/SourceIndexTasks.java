package org.renjin.ci.source.index;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageSource;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.parser.RParser;
import org.renjin.sexp.SEXP;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceIndexTasks {

  private static final Logger LOGGER = Logger.getLogger(SourceIndexTasks.class.getName());
  
  private static final DatastoreService DATASTORE = DatastoreServiceFactory.getDatastoreService();
  
  private static final int MAX_SOURCE_SIZE = 90 * 1024;
  public static final String QUEUE_NAME = "source-index";

  @POST
  @Path("enqueueRebuild")
  public Response enqueueRebuild() {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/source/index/rebuild"));
    return Response.ok("Enqueued.").build();
  }
  
  @POST
  @Path("rebuild")
  public Response enqueueAllPackages() {
    Queue queue = QueueFactory.getDefaultQueue();

    for (Key<PackageVersion> key : PackageDatabase.getPackageVersionIds()) {
      PackageVersionId id = PackageVersion.idOf(key);
      enqueuePackageForSourceIndexing(id);
    }

    return Response.ok("Enqueued").build();
  }

  public static void enqueuePackageForSourceIndexing(PackageVersionId id) {
    QueueFactory.getQueue(QUEUE_NAME).add(TaskOptions.Builder
        .withUrl("/source/index/extract")
        .param("packageVersionId", id.toString())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(10)));

    QueueFactory.getQueue(QUEUE_NAME).add(TaskOptions.Builder
        .withUrl("/source/index/countLines")
        .param("packageVersionId", id.toString())
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(10)));
  }
  
  @POST
  @Path("countLines")
  public Response countLines(@FormParam("packageVersionId") String packageVersionId) throws IOException {

    Entity entity = LocCounter.computeLoc(PackageVersionId.fromTriplet(packageVersionId));
    
    DatastoreServiceFactory.getDatastoreService().put(entity);

    return Response.ok().build();
  }

  /**
   * Extracts all R sources from the source archive
   */
  @POST
  @Path("extract") 
  public Response reindex(@FormParam("packageVersionId") String packageVersionString) throws IOException {

    PackageVersionId id = new PackageVersionId(packageVersionString);
    
    AppEngineSourceArchiveProvider provider = new AppEngineSourceArchiveProvider();

    String packagePrefix = id.getPackageName() + "/";

    Queue queue = QueueFactory.getQueue(QUEUE_NAME);

    DatastoreMutationPool mutationPool = DatastoreMutationPool.create();

    try (TarArchiveInputStream tarIn = provider.openSourceArchive(id)) {
      TarArchiveEntry entry;
      while ((entry = tarIn.getNextTarEntry()) != null) {
        if (entry.getName().startsWith(packagePrefix) && isIndexable(entry)) {
          
          String filename = entry.getName().substring(packagePrefix.length());

          if (entry.getRealSize() > MAX_SOURCE_SIZE) {
            LOGGER.log(Level.WARNING, String.format("%s in %s exceeds maximum file size: %d",
                filename,
                packageVersionString,
                entry.getRealSize()));
          } else {
          
            String source = new String(ByteStreams.toByteArray(tarIn), Charsets.UTF_8);

            try {

              Key<PackageVersion> parentKey = PackageVersion.key(id);

              // Save the source to a single entity
              Entity sourceEntity = new Entity("PackageSource", filename, parentKey.getRaw());
              sourceEntity.setProperty("source", new Text(source));
              
              mutationPool.put(sourceEntity);
              
              queue.add(TaskOptions.Builder.withUrl("/source/index/file")
                  .param("packageVersionId", packageVersionString)
                  .param("filename", filename));
              
              
            } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Exception enqueuing index task for " + filename +
                " in " + packageVersionString + " (" + source.length() + " chars)", e);
            }
          }
        }
      }
    }
    
    mutationPool.flush();
    
    return Response.ok().build();
  }

  private boolean isIndexable(TarArchiveEntry entry) {
    String name = entry.getName().toUpperCase();
    return name.endsWith(".R") | name.endsWith(".S");
  }

  /**
   * Parse the stored source file and record function uses and definitions
   */
  @POST
  @Path("file")
  public Response indexSource(@FormParam("packageVersionId") String packageVersionString, 
                              @FormParam("filename") String filename) {
    

    PackageVersionId packageVersionId = PackageVersionId.fromTriplet(packageVersionString);
    LoadResult<PackageSource> sourceEntity = PackageDatabase.getPackageSource(packageVersionId, filename);

    // Try to parse source
    SEXP source;
    try {
      source = RParser.parseAllSource(new StringReader(sourceEntity.safe().getSource()));
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to parse " + filename + " in " + packageVersionId +
        ": " + e.getMessage());
      return Response.ok().build();
    }

    UseVisitor useVisitor = new UseVisitor();
    source.accept(useVisitor);

    DefVisitor defVisitor = new DefVisitor();
    source.accept(defVisitor);

    // Index uses
    Entity indexEntity = new Entity("FunctionIndex",
        packageVersionId.getPackageId() + "/" +
            filename + "@" + packageVersionId.getVersionString());
    indexEntity.setProperty("use", Lists.newArrayList(useVisitor.getResult()));
    indexEntity.setProperty("def", Lists.newArrayList(defVisitor.getResult()));

    DATASTORE.put(indexEntity);
    
    return Response.ok().build();
  }
}
