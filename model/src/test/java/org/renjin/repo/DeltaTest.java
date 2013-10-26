package org.renjin.repo;

import org.junit.Test;

public class DeltaTest {

  @Test
  public void test() {

    Predecessors preds = new Predecessors(PersistenceUtil.createEntityManager(),
      "412d8b64b20eef7cdab7fec380d0bc1bbb679edc");
    System.out.println(preds.getPredecessors());
  }
}
