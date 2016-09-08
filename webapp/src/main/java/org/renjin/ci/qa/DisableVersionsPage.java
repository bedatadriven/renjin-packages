package org.renjin.ci.qa;

import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisableVersionsPage {


  private final PackageId packageId;
  private final List<PackageVersion> versions = new ArrayList<>();

  public DisableVersionsPage(PackageId packageId) {
    this.packageId = packageId;

    List<PackageVersion> packageVersions = PackageDatabase.getPackageVersions(packageId);
    Collections.sort(packageVersions);

    for (PackageVersion packageVersion : packageVersions) {
      versions.add(packageVersion);
    }
  }

  public PackageId getPackageId() {
    return packageId;
  }

  public List<PackageVersion> getVersions() {
    return versions;
  }
}
