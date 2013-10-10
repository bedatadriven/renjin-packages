package org.renjin.repo.task;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class CRANTest {

  @Test
  @Ignore
  public void test() {

    UpdateCranPackagesTask task = new UpdateCranPackagesTask();
    task.updateIndex();
  }

  @Test
  public void fetch() throws IOException {


    UpdateCranPackagesTask task = new UpdateCranPackagesTask();
    task.fetchPackage("survey", "3.29-5");

  }

}
