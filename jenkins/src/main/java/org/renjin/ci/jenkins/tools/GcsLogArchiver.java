package org.renjin.ci.jenkins.tools;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import hudson.FilePath;
import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Archives build logs and test output to GCS
 */
public class GcsLogArchiver implements LogArchiver {
  private BuildContext buildContext;
  private final Storage storage;
  private final PackageVersionId packageVersionId;
  private final String buildNumber;


  public GcsLogArchiver(BuildContext buildContext, Storage storage) {
    this.buildContext = buildContext;
    this.storage = storage;
    this.packageVersionId = buildContext.getPackageVersionId();
    this.buildNumber = buildContext.getBuildNumber();
  }


  @Override
  public void archiveLog() throws IOException {
    String objectName = StorageKeys.buildLog(packageVersionId, buildNumber);
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

  @Override
  public void archiveTestOutput(String testName, FilePath outputFile) throws IOException {
    String objectName = StorageKeys.testLog(packageVersionId, buildNumber, testName);

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

  public void archivePlots(FilePath plotDir) throws IOException, InterruptedException {
    for (FilePath filePath : plotDir.list()) {
      archivePlot(filePath);
    }
  }

  private void archivePlot(FilePath filePath) throws IOException {
    String objectName = "plot/" + filePath.getName();
    String contentType = "image/svg+xml";

    StorageObject objectMetadata = new StorageObject()
        .setName(objectName)
        .setContentType(contentType)
        .setCacheControl("public immutable");

    Storage.Objects.Insert request = storage.objects().insert(
        StorageKeys.BUILD_LOG_BUCKET,
        objectMetadata,
        new FilePathContent(contentType, filePath));

    // Setting to 0 makes the operation succeed only if there are no live versions of the object.
    request.setIfGenerationMatch(0L);


    request.setPredefinedAcl("publicread");
    try {
      request.execute();
    } catch (GoogleJsonResponseException e) {
      if(e.getStatusCode() == 412) {
        // Already present, no need to upload
      }
    }
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

