package org.renjin.ci.releases;

import org.renjin.ci.datastore.PackageDatabase;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/install.R")
public class InstallResource {

  @GET
  public String getInstallScript() {
    String version = PackageDatabase.getLatestRelease().toString();
    return String.format("install.packages(\"https://nexus.bedatadriven.com/content/groups/public/org/renjin/renjin-gnur-package/%s/renjin-gnur-package-%s.tar.gz\", repos = NULL)\n",
        version,
        version);
  }

}
