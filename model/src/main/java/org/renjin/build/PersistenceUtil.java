package org.renjin.build;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class PersistenceUtil {

  public static EntityManagerFactory emf;

  public static EntityManager createEntityManager() {
    if(emf == null) {
      emf = Persistence.createEntityManagerFactory("renjin-repo");
    }
    return emf.createEntityManager();
  }

}
