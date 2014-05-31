package org.renjin.build;

import org.junit.Ignore;
import org.junit.Test;
import org.renjin.build.model.Build;

import javax.persistence.EntityManager;
import java.util.List;

public class DeltaTest {

  @Ignore
  @Test
  public void test() {


    EntityManager em = PersistenceUtil.createEntityManager();

//    new DeltaCalculator(em, 87).calculate();


    List<Build> builds = em.createQuery("select b from Build b", Build.class)
      .getResultList();

    for(Build build : builds) {
      System.out.println("#" + build.getId());
      new DeltaCalculator(em, build.getId()).calculate();
    }

  }
}
