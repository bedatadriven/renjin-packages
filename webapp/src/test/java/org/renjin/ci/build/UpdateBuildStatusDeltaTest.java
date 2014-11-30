package org.renjin.ci.build;

import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.model.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class UpdateBuildStatusDeltaTest extends AbstractDatastoreTest {

  private PackageVersionId versionId;

  @Test
  public void update() {

    versionId = new PackageVersionId("org.renjin.cran", "MASS", "14.0");

    saveStatus("1.0", BuildStatus.BUILT);
    saveStatus("1.1", BuildStatus.READY);
    saveStatus("1.2", BuildStatus.FAILED);
    saveStatus("2.0", BuildStatus.BUILT);
    saveStatus("10.4", BuildStatus.FAILED);

    UpdateBuildStatusDelta fn = new UpdateBuildStatusDelta();
    fn.apply(new PackageVersion(versionId));

    assertThat(queryDelta("1.2"), equalTo(Delta.REGRESSION));
    assertThat(queryDelta("2.0"), equalTo(Delta.IMPROVEMENT));
    assertThat(queryDelta("10.4"), equalTo(Delta.REGRESSION));
  }

  private void saveStatus(String renjinVersion, BuildStatus building) {
    PackageStatus status = new PackageStatus(versionId, new RenjinVersionId(renjinVersion));
    status.setBuildStatus(building);
    PackageDatabase.save(status);
  }

  private Delta queryDelta(String renjinVersion) {
    PackageStatus status = PackageDatabase.getStatus(versionId, new RenjinVersionId(renjinVersion));
    return Delta.valueOf(status.getBuildDelta());
  }


}