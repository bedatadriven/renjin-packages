package org.renjin.ci.repo.maven;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.googlecode.objectify.Key;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.repo.apt.AptArtifact;
import org.renjin.ci.storage.StorageKeys;

public class MavenArtifactRequest {

  private String groupId;
  private String artifactId;
  private String version;
  private String filename;

  public MavenArtifactRequest(String groupId, String artifactId, String version, String filename) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.filename = filename;
  }

  public static MavenArtifactRequest parse(String path) {
    String[] parts = path.split("/");

    int partIndex = parts.length - 1;
    String filename = parts[partIndex--];
    String version = parts[partIndex--];
    String artifactId = parts[partIndex--];
    StringBuilder group = new StringBuilder();
    for (int i = 0; i <= partIndex; i++) {
      if(i > 0) {
        group.append(".");
      }
      group.append(parts[i]);
    }

    return new MavenArtifactRequest(group.toString(), artifactId, version, filename);
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getFilename() {
    return filename;
  }

  public BlobKey getBlobKey() {
    return BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(
        "/gs/" + StorageKeys.ARTIFACTS_BUCKET + "/" + groupId + "/" + artifactId + "/" + version + "/" + filename);
  }
}
