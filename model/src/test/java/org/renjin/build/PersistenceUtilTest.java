package org.renjin.build;

import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.EntityManager;


public class PersistenceUtilTest {

  @Ignore
  @Test
  public void test() {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.createQuery("select p from RPackage p").getResultList();

  }



}
