package org.renjin.repo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.logging.Logger;

public class HibernateUtil {

  private static final EntityManagerFactory EMF =
          Persistence.createEntityManagerFactory("renjin-repo-gae"); 
  
  private static final ThreadLocal<EntityManager> EM = new ThreadLocal<EntityManager>();
  
  private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());
  
  public static EntityManager getEntityManager() {

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
