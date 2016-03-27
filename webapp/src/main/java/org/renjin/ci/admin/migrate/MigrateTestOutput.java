package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
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

  private static final Logger LOGGER = Logger.getLogger(MigrateTestOutput.class.getName());
  
  private transient GcsService client;
  

  @Override
  public String getEntityKind() {
    return "PackageTestResult";
  }

  @Override
  public void beginSlice() {
    client = GcsServiceFactory.createGcsService();
  }
  
  @Override
  public void map(Entity entity) {

    migrateEntity(entity);
  }

  public void migrateEntity(Entity entity) {
    Object outputObject = entity.getProperty("output");

    if(outputObject instanceof String) {
      String output = (String) outputObject;
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

        LOGGER.info("Writing to " + filename);

        try (OutputStream outputStream = newOutputStream(client.createOrReplace(filename, options))) {
          outputStream.write(output.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

}
