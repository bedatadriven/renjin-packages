package org.renjin.ci.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A package dependency resolved to a fully qualified {@code PackageVersionId} and 
 * a build number, if one is available.
 */
@JsonAutoDetect(
    isGetterVisibility = JsonAutoDetect.Visibility.NONE, 
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ResolvedDependency {
  
  @JsonProperty
  private String scope = "compile";
  
  @JsonProperty
  private PackageVersionId packageVersionId;
  
  @JsonProperty
  private Long buildNumber;

  public ResolvedDependency() {
  }
  
  public ResolvedDependency(PackageBuildId buildId) {
    this.packageVersionId = buildId.getPackageVersionId();
    this.buildNumber = buildId.getBuildNumber();
  }

  public ResolvedDependency(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }
  
  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }

  public void setPackageVersionId(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public Long getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(Long buildNumber) {
    this.buildNumber = buildNumber;
  }

  @Override
  public String toString() {
    if(buildNumber == null) {
      return packageVersionId.toString();
    } else {
      return packageVersionId + "-b" + buildNumber;
    }
  }

  @JsonIgnore
  public boolean hasBuild() {
    return buildNumber != null;
  }

  public PackageBuildId getBuildId() {
    return new PackageBuildId(packageVersionId, buildNumber);
  }
}
