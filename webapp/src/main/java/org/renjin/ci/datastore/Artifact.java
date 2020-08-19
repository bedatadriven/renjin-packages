package org.renjin.ci.datastore;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Maven Artifact
 */
@Entity
public class Artifact {


  @Id
  private String path;

  @Index
  private String release;

  Blob content;


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getRelease() {
    return release;
  }

  public void setRelease(String release) {
    this.release = release;
  }

  public Blob getContent() {
    return content;
  }

  public void setContent(Blob content) {
    this.content = content;
  }
}
