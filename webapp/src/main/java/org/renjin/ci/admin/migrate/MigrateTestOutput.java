package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.*;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.common.base.Charsets;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachEntity;
import org.renjin.ci.storage.StorageKeys;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import static java.nio.channels.Channels.newOutputStream;

/**
 * Move test results to google storage
 */
public class MigrateTestOutput extends ForEachEntity {

  private transient Logger LOGGER;

  private transient GcsService client;
  private transient DatastoreService datastore;
  private DatastoreMutationPool mutationPool;

  @Override
  public void beginSlice() {
    client = GcsServiceFactory.createGcsService();
    datastore = DatastoreServiceFactory.getDatastoreService();
    mutationPool = DatastoreMutationPool.create();
    
    LOGGER = Logger.getLogger(MigrateTestOutput.class.getName());
  }

  @Override
  public String getEntityKind() {
    return "PackageTestResult";
  }

  public void map(Entity entity) {

    Object outputObject = entity.getProperty("output");

    String output = "";
    if(outputObject instanceof Text) {
      output = ((Text) outputObject).getValue();
    } else if(outputObject instanceof String) {
      output = (String) outputObject;
    }
    if(output.length() > 0) {

      String testName = entity.getKey().getName();

      Key buildKey = entity.getParent();
      long buildNumber = buildKey.getId();

      Key packageVersionKey = buildKey.getParent();
      String version = packageVersionKey.getName();

      Key packageKey = packageVersionKey.getParent();
      PackageId packageId = PackageId.valueOf(packageKey.getName());
      PackageVersionId packageVersionId = new PackageVersionId(packageId, version);

      GcsFilename filename = new GcsFilename(StorageKeys.BUILD_LOG_BUCKET,
          StorageKeys.testLog(packageVersionId, buildNumber, testName));

      GcsFileOptions options = new GcsFileOptions.Builder()
          .mimeType("text/plain")
          .acl("public-read").build();

      try (OutputStream outputStream = newOutputStream(client.createOrReplace(filename, options))) {
        outputStream.write(output.getBytes(Charsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      LOGGER.info("Wrote to " + filename);

      entity.removeProperty("output");

      datastore.put(entity);
    }
  }

}
