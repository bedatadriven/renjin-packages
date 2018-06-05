package org.renjin.ci;


import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.StringWriter;

@Path("/robots.txt")
public class RobotsResource {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String get(@HeaderParam("Host") String host) {
    // Only allow indexing of the principle domain
    StringWriter writer = new StringWriter();
    writer.append("User-agent: *\n");
    writer.append("Crawl-delay: 1000\n");
    if(!host.equals("packages.renjin.org")) {
      writer.append("Disallow: /").append("\n");
    }
    return writer.toString();
  }

}
