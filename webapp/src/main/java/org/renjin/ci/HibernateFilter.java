package org.renjin.ci;

import javax.servlet.*;
import java.io.IOException;

public class HibernateFilter implements Filter {


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      HibernateUtil.cleanup();
    }
  }

  @Override
  public void destroy() {
  }
}
