package org.renjin.build.fetch;

import org.junit.Ignore;
import org.junit.Test;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;

public class DownstreamCountTaskTest {

  @Test
  @Ignore
  public void test() {

    EntityManager em = HibernateUtil.createEntityManager();
    RPackageVersion mass = em.find(RPackageVersion.class, "org.renjin.cran:MASS:7.3-29");

    System.out.println(DownstreamCountTask.countDownstream(mass));

  }
}
