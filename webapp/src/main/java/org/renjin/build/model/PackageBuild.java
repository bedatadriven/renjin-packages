package org.renjin.build.model;

import com.google.common.collect.Sets;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Records metadata about an individual package
 * build attempt
 */
@Entity
public class PackageBuild {

  @Id
  private String id;

  private BuildOutcome outcome;

  /**
   * The Renjin version against which the
   * package was built
   */
  private String renjinVersion;

  @Unindex
  private Set<String> dependencies;

  private String pom;

  public PackageBuild() {
  }

  public PackageBuild(PackageVersionId packageVersionId, long buildNumber) {
    this.id = packageVersionId.toString() + ":" + buildNumber;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  @Transient
  public String getPackageName() {
    String[] parts = id.split(":");
    return parts[1];
  }


  public PackageVersionId getPackageVersionId() {
    String[] parts = id.split(":");
    return new PackageVersionId(parts[0], parts[1], parts[2]);
  }

  public long getBuildNumber() {
    String[] parts = id.split(":");
    return Long.parseLong(parts[3]);
  }

  @Transient
  public boolean isSucceeded() {
    return outcome == BuildOutcome.SUCCESS;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public void setRenjinVersion(RenjinVersionId release) {
    this.renjinVersion = release.toString();
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

  public String getPom() {
    return pom;
  }

  public void setPom(String pom) {
    this.pom = pom;
  }


}
