package org.renjin.ci.tasks;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Ignore;
import org.junit.Test;
import org.renjin.ci.index.CRAN;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertThat;


public class CRANTest {

  @Test
  public void parseUpdateList() throws IOException {

    List<String> packageEntries = CRAN.parseUpdatedPackageList(new LocalDate(2014, 5, 31),
        Resources.asByteSource(Resources.getResource("packages_by_date.html")));

    assertThat(packageEntries, Matchers.equalTo(Arrays.asList(
        "ChemoSpec", "ggtern", "IsoCI", "knitcitations", "lasso2", "pbo", "pipeR", "qdap",
        "qdapDictionaries", "ripa", "rplotengine", "statmod", "STPGA", "taRifx.geo",
        "transport")));
  }

  @Test
  @Ignore("requires fetch")
  public void fetchArchiveVersions() throws IOException {
    Set<PackageVersionId> survival = CRAN.getArchivedVersionList("survival");
    
    System.out.println(survival);
  }
  
  @Test
  public void parseVersion() throws IOException {

    String version = CRAN.parsePackageVersion(
        Resources.asByteSource(Resources.getResource("package_detail.html")));

    assertThat(version, Matchers.equalTo("1.3.4"));
  }

  @Test
  public void parseVersionNew() throws IOException {

    String version = CRAN.parsePackageVersion(
        Resources.asByteSource(Resources.getResource("package_detail_new.html")));

    assertThat(version, Matchers.equalTo("2.0"));
  }
}
