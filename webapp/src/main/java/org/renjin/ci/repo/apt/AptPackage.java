package org.renjin.ci.repo.apt;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class AptPackage {

  @Id
  private String name;

  private String latestVersion;

  private Key<AptArtifact> latestArtifact;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public Key<AptArtifact> getLatestArtifact() {
    return latestArtifact;
  }

  public void setLatestArtifact(Key<AptArtifact> latestArtifact) {
    this.latestArtifact = latestArtifact;
  }
}
