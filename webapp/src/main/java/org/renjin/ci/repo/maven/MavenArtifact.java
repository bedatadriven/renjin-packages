package org.renjin.ci.repo.maven;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

@Entity
public class MavenArtifact {

  @Id
  private String objectName;

  @Index
  private String groupArtifactPath;

  private Date lastModified;

  private String filename;

  private long size;

  public String getGroupArtifactPath() {
    return groupArtifactPath;
  }

  public void setGroupArtifactPath(String groupArtifactPath) {
    this.groupArtifactPath = groupArtifactPath;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String name) {
    this.filename = name;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }
}
