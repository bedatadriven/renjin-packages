package org.renjin.repo;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class HibernateResponseFilter implements ContainerRequestFilter {

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    HibernateUtil.cleanup();
    return request;
  }
}
