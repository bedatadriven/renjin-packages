package org.renjin.build;

import com.google.common.base.Strings;
import org.hibernate.StatelessSession;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.logging.Logger;

public class HibernateUtil {

  private static final EntityManagerFactory EMF =
          Persistence.createEntityManagerFactory(getPersistenceUnitName());

  private static String getPersistenceUnitName() {

    String environment = System.getProperty("com.google.appengine.runtime.environment");
    if("Production".equals(environment)) {
      // appengine production
      return "renjin-repo-gae";
    } else {
      return "renjin-repo";
    }
  }

  private static final ThreadLocal<EntityManager> EM = new ThreadLocal<EntityManager>();
  
  private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());
  
  public static EntityManager createEntityManager() {
    return EMF.createEntityManager();
  }
  
  public static EntityManager getActiveEntityManager() {
    EntityManager em = EM.get();
    if(em == null) {
      LOGGER.info("Creating EntityManager for request");
      em = EMF.createEntityManager();
      EM.set(em);
    }
    return em;
  }

  public static StatelessSession openStatelessSession() {
    return ((HibernateEntityManagerFactory)EMF).getSessionFactory().openStatelessSession();
  }
  
  public static void cleanup() {
    EntityManager em = EM.get();
    if(em != null) {
      LOGGER.info("Cleaning up EntityManager");
      try {
        em.close();
      } finally {
        EM.remove();
      }
    }
  }
}
