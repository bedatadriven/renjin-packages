package org.renjin.ci.packages;

import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersion;

public class VersionViewModel {

  private final PackageDescription description;
  private PackageVersion packageVersion;

  public VersionViewModel(PackageVersion packageVersion) {
    this.packageVersion = packageVersion;
    this.description = packageVersion.parseDescription();
  }

  public String getPackageName() {
    return packageVersion.getPackageVersionId().getPackageName();
  }

  public String getTitle() {
    return description.getTitle();
  }

  public String getDescription() {
    return description.getDescription();
  }

  public String getVersion() {
    return packageVersion.getVersion().toString();
  }

}
