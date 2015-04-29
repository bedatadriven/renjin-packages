package org.renjin.ci.workflow.queue;

import org.renjin.ci.model.PackageVersionId;

public class PackageLease {
  private final PackageVersionId packageVersionId;

  public PackageLease(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  public PackageVersionId getPackageVersionId() {
    return packageVersionId;
  }
}
