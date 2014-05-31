package org.renjin.repo;

import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


public class PersistenceUtilTest {

  @Ignore
  @Test
  public void test() {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.createQuery("select p from RPackage p").getResultList();

  }



}
