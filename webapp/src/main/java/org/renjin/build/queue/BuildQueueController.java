package org.renjin.build.queue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Supervises the build queue:
 * <ul>
 *   <li>Marks dependencies as resolved</li>
 *   <li>Frees leases on timedout workers</li>
 * </ul>
 */
public class BuildQueueController extends HttpServlet {


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doGet(req, resp);
  }
}
