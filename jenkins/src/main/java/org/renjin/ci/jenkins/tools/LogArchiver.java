package org.renjin.ci.jenkins.tools;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import hudson.FilePath;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.storage.StorageKeys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Archives build logs and test outpu to GCS
 */
public class LogArchiver {
  private BuildContext buildContext;
  private final PackageBuild build;
  private final Storage storage;

  public LogArchiver(BuildContext buildContext, PackageBuild build, Storage storage) {
    this.buildContext = buildContext;
    this.build = build;
    this.storage = storage;
  }

  public void archiveLog() throws IOException {
    String objectName = StorageKeys.buildLog(build.getPackageVersionId(), build.getBuildNumber());
    File logFile = buildContext.getLogFile();

    StorageObject objectMetadata = new StorageObject()
        .setName(objectName)
        .setContentType("text/plain")
        .setContentEncoding("gzip");

    Storage.Objects.Insert request = storage.objects().insert(
        StorageKeys.BUILD_LOG_BUCKET,
        objectMetadata,
        new FileContent("text/plain", logFile));

    request.setPredefinedAcl("publicread");
    request.setContentEncoding("gzip");
    request.execute();

    buildContext.log("Successfully archived log file");
  }

  public void archiveTestOutput(String testName, FilePath outputFile) throws IOException {
    String objectName = StorageKeys.testLog(build.getPackageVersionId(), build.getBuildNumber(), testName);

    StorageObject objectMetadata = new StorageObject()
        .setName(objectName)
        .setContentType("text/plain");

    Storage.Objects.Insert request = storage.objects().insert(
        StorageKeys.BUILD_LOG_BUCKET,
        objectMetadata,
        new FilePathContent("text/plain", outputFile));

    request.setPredefinedAcl("publicread");
    request.execute();
  }


  private static class FilePathContent extends AbstractInputStreamContent {

    private FilePath filePath;

    public FilePathContent(String type, FilePath filePath) {
      super(type);
      this.filePath = filePath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      try {
        return filePath.read();
      } catch (InterruptedException e) {
        throw new IOException("Interrupted");
      }
    }

    @Override
    public long getLength() throws IOException {
      try {
        return filePath.length();
      } catch (InterruptedException e) {
        throw new IOException("Interrupted");
      }
    }

    @Override
    public boolean retrySupported() {
      return true;
    }
  }
}

