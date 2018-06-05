package org.renjin.ci;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class NoRobotsFilter implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext context) throws IOException {
    if(isUserAgentBot(context.getHeaderString("User-Agent"))) {
      context.abortWith(forbidden());
    }
  }

  public static Response forbidden() {
    return Response.status(Response.Status.FORBIDDEN)
        .entity("403: Sorry, this page is not intended to be indexed by crawlers/bots!")
        .build();
  }

  public static boolean isUserAgentBot(String userAgentString) {
    if(userAgentString == null) {
      return false;
    }
    String agentLowered = userAgentString.toLowerCase();
    return agentLowered.contains("bot") ||
           agentLowered.equals("ia_archiver");
  }

  public static void checkNotRobot(String userAgent) {
    if(isUserAgentBot(userAgent)) {
      throw new WebApplicationException(forbidden());
    }
  }
}
