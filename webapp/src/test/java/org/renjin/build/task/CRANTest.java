package org.renjin.build.task;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Test;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
  public void parseVersion() throws IOException {

    String version = CRAN.parsePackageVersion(
        Resources.asByteSource(Resources.getResource("package_detail.html")));

    assertThat(version, Matchers.equalTo("1.3.4"));
  }

  @Test
  public void integrationTest() throws IOException {

    CranTasks tasks = new CranTasks();
    tasks.fetchPackage("qtl");

  }

}
