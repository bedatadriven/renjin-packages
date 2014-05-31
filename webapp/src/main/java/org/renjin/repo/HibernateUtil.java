package org.renjin.repo;

import com.google.common.base.Strings;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.logging.Logger;

public class HibernateUtil {

  private static final EntityManagerFactory EMF =
          Persistence.createEntityManagerFactory(getPersistenceUnitName());

  private static String getPersistenceUnitName() {

    if(Strings.isNullOrEmpty(System.getProperty("com.google.appengine.runtime.environment"))) {
      // unit tests
      return "renjin-repo";
    } else {
      // appengine dev or production
      return "renjin-repo-gae";
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
  
  public static void cleanup() {
    EntityManager em = EM.get();
    if(em != null) {
      LOGGER.info("Cleaning up EntityManager");
      em.close();
      EM.remove();
    }
  }
}
