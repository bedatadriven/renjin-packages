package org.renjin.ci.workflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageBuild {
  private PackageBuildId id;
  private RenjinVersionId renjinVersionId;

  @JsonCreator
  public PackageBuild(@JsonProperty("id") PackageBuildId id) {
    this.id = id;
  }
  
  public PackageBuild(PackageVersionId packageVersionId, long buildNumber) {
    this.id = new PackageBuildId(packageVersionId, buildNumber);
  }

  public PackageBuildId getId() {
    return id;
  }
  
  public PackageVersionId getPackageVersionId() {
    return id.getPackageVersionId();
  }


  public long getBuildNumber() {
    return id.getBuildNumber();
  }

  public RenjinVersionId getRenjinVersionId() {
    return renjinVersionId;
  }

  public void setRenjinVersionId(RenjinVersionId renjinVersionId) {
    this.renjinVersionId = renjinVersionId;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersionId = RenjinVersionId.valueOf(renjinVersion);
  }

  public String getBuildVersion() {
    return id.getBuildVersion();
  }
}
