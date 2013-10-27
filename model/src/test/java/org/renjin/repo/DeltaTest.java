package org.renjin.repo;

import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

public class DeltaTest {

  @Test
  public void test() {

    new DeltaCalculator(PersistenceUtil.createEntityManager(), 52).calculate();

  }
}
