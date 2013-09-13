package org.renjin.repo.task;

import org.junit.Ignore;
import org.junit.Test;

public class CRANTest {

  @Test
  @Ignore
  public void test() {

    UpdateCranPackagesTask task = new UpdateCranPackagesTask();
    task.fetchList();
  }

}
