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

  @Test
  @Ignore
  public void jdbcTest() throws SQLException {
    DriverManager.registerDriver(new com.google.cloud.sql.Driver());

    Properties props = new Properties();
    props.put("oauth2ClientId", "32555940559.apps.googleusercontent.com");
    props.put("oauth2ClientSecret", "ZmssLNjJy2998hD4CTg2ejr2");

    Connection connection = DriverManager.getConnection("jdbc:google:rdbms://bdd-renjin:renjin-repo/repo", props);
    Statement stmt = connection.createStatement();
    stmt.executeQuery("select count(*) from RPackageVersion");
    stmt.close();
    connection.close();

  }


}
