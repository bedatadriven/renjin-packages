package org.renjin.infra.agent.util;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class PersistenceUtil {

  public static EntityManagerFactory EMF = null;

  public static EntityManager createEntityManager() {
    if(EMF == null) {
      EMF = Persistence.createEntityManagerFactory("renjin-repo");
    }
    return EMF.createEntityManager();
  }
}
