package org.renjin.ci.workflow.tools;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Preconditions;
import hudson.AbortException;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependencySet;
import org.renjin.ci.workflow.PackageBuild;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WebApp {

  private static final Logger LOGGER = Logger.getLogger(WebApp.class.getName());

  public static final String ROOT_URL = "https://renjinci.appspot.com";


  private static WebTarget rootTarget() {
    return client().target(ROOT_URL);
  }

  private static Client client() {
    return ClientBuilder.newClient().register(JacksonJsonProvider.class);
  }
  
  
  public static PackageBuild startBuild(PackageVersionId packageVersionId, String renjinVersion) throws IOException, InterruptedException {
    long buildNumber = postBuild(packageVersionId, renjinVersion);
    PackageBuild build = new PackageBuild(packageVersionId, buildNumber);
    build.setRenjinVersion(renjinVersion);
    return build;
  }

  public static ResolvedDependencySet resolveDependencies(PackageVersionId packageVersionId) {
    return packageVersion(packageVersionId)
        .path("resolveDependencies")
        .request().get(ResolvedDependencySet.class);
    
  }


  public static long postBuild(PackageVersionId packageVersionId, String renjinVersion) throws AbortException {
    Preconditions.checkNotNull(renjinVersion, "renjinVersion cannot be null");
    
    PackageBuild build;Form form = new Form();
    form.param("renjinVersion", renjinVersion);

    WebTarget builds = packageVersion(packageVersionId).path("builds");
    try {

      build = builds.request(MediaType.APPLICATION_JSON)
          .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), PackageBuild.class);


    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception getting build number", e);
      throw new AbortException("Failed to get next build number from " + builds.getUri() + ": " + e.getMessage());
    }
    return build.getBuildNumber();
  }

  private static WebTarget packageVersion(PackageVersionId packageVersionId) {
    return rootTarget()
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString());
  }


  public static void postResult(PackageBuild build, PackageBuildResult result) {
    PackageVersionId packageVersionId = build.getPackageVersionId();
    WebTarget buildResource = client().target(ROOT_URL)
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString())
        .path("build")
        .path(Long.toString(build.getBuildNumber()));

    buildResource.request().post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));
  }

  public static List<PackageVersionId> queryPackageList(String filter, Map<?, ?> filterParameters) {
    WebTarget target = rootTarget().path("packages").path(filter);
    if(filterParameters != null) {
      for (Map.Entry<?, ?> parameter : filterParameters.entrySet()) {
        target = target.queryParam(parameter.getKey().toString(), parameter.getValue());
      }
    }
    String[] ids = target.request().get(String[].class);
    
    List<PackageVersionId> packageVersionIds = new ArrayList<PackageVersionId>();
    for (String id : ids) {
      packageVersionIds.add(PackageVersionId.fromTriplet(id));
    }
    return packageVersionIds;
  }
}
