package org.renjin.repo.task;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class CRANTest {

  @Test
  @Ignore
  public void test() {

    CranTasks task = new CranTasks();
    task.updateIndex();
  }

  @Test
  public void fetch() throws IOException {


    CranTasks task = new CranTasks();
    task.fetchPackage("survey", "3.29-5");

  }

}
