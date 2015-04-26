package org.renjin.ci.build;

import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

/**
 * Resets Package Status
 */
public class ResetPackageStatus extends ForEachEntityAsBean<PackageVersion> {

  public ResetPackageStatus() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion packageVersion) {
    PackageCheckQueue.createStatus(packageVersion, RenjinVersionId.RELEASE);
  }
}
