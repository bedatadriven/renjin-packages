package org.renjin.build.task;

import org.junit.Ignore;
import org.junit.Test;

public class CalculateDependenciesTaskTest {

  @Ignore
  @Test
  public void test() {

    CalculateDependenciesTask task = new CalculateDependenciesTask(new RemoteSourceArchiveProvider());
    task.calculate("org.renjin.cran:ALS");
  }
}
