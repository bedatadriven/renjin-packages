package org.renjin.build.model;

import com.google.appengine.api.datastore.Entity;
import org.renjin.build.AbstractDatastoreTest;

import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class PackageDatabaseTest extends AbstractDatastoreTest {


  @org.junit.Test
  public void queryVersions() {

    // some test packages
    PackageVersion ggplot_1 = new PackageVersion();
    ggplot_1.setId("org.renjin.cran:ggplot:1.4-34");

    PackageVersion ggplot_2 = new PackageVersion();
    ggplot_2.setId("org.renjin.cran:ggplot:5.4-34");

    PackageVersion ggplz = new PackageVersion();
    ggplz.setId("org.renjin.cran:ggplz-earth:5.4-34");

    PackageDatabase.save(ggplot_1, ggplot_2, ggplz).now();

    assertThat("org.renjin.cran:ggplot:1.4-34", greaterThan("org.renjin.cran:ggplot"));

    // ensure that we can fetch
    List<PackageVersion> versions = PackageDatabase.queryPackageVersions("org.renjin.cran", "ggplot");

    assertThat(versions, hasSize(2));

  }

}
