package org.renjin.ci.workflow.tools;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import hudson.AbortException;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.workflow.BuildPackageStep;
import org.renjin.ci.workflow.PackageBuild;
import org.renjin.ci.workflow.PackageBuildContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;


public class WebApp {

  private static final Logger LOGGER = Logger.getLogger(WebApp.class.getName());

  public static final String ROOT_URL = "https://renjinci.appspot.com";

  public static long startBuild(BuildPackageStep step) throws IOException, InterruptedException {

    return postBuild(step, step.getPackageVersionId());
  }

  public static void reportBuildResult(PackageBuildContext context, PackageBuildResult result) throws IOException {
    postResult(context, result);
  }

  public static List<ResolvedDependency> resolveDependencies(PackageVersionId packageVersionId) {
    return asList(packageVersion(packageVersionId)
        .path("resolveDependencies")
        .request().get(ResolvedDependency[].class));
    
  }


  private static long postBuild(BuildPackageStep step, PackageVersionId packageVersionId) throws AbortException {
    PackageBuild build;Form form = new Form();
    form.param("renjinVersion", step.getRenjinVersion());

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
    Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);

    return client.target(ROOT_URL)
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString());
  }

  private static void postResult(PackageBuildContext build, PackageBuildResult result) {
    build.getLogger().println("Native source compilation result: " + result.getNativeOutcome());
    build.getLogger().println("Posting build result " + result.getOutcome() + "...");

    Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
    PackageVersionId packageVersionId = build.getPackageVersionId();
    WebTarget buildResource = client.target(ROOT_URL)
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString())
        .path("build")
        .path(Long.toString(build.getBuildNumber()));

    buildResource.request().post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));
  }
}
