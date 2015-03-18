package org.renjin.ci.workflow.tools;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.renjin.ci.model.PackageBuild;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.task.PackageBuildResult;
import org.renjin.ci.workflow.BuildPackageStep;
import org.renjin.ci.workflow.PackageBuildContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Reader;


public class WebApp {


    public static final String ROOT_URL = "https://renjinci.appspot.com";

    public static long startBuild(BuildPackageStep step) throws IOException, InterruptedException {

        PackageVersionId packageVersionId = PackageVersionId.fromTriplet(step.getPackageVersionId());
        return postBuild(step, packageVersionId);
    }
    
    public static void reportBuildResult(PackageBuildContext context, PackageBuildResult result) throws IOException {
        postResult(context, result);
    }



    private static long postBuild(BuildPackageStep step, PackageVersionId packageVersionId) throws AbortException {
        PackageBuild build;Form form = new Form();
        form.param("renjinVersion", step.getRenjinVersion());

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        WebTarget builds = client.target(ROOT_URL)
                .path("package")
                .path(packageVersionId.getGroupId())
                .path(packageVersionId.getPackageName())
                .path(packageVersionId.getVersionString())
                .path("builds");
        try {

            build = builds.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), PackageBuild.class);


        } catch (Exception e) {
            throw new AbortException("Failed to get next build number from " + builds.getUri() + ": " + e.getMessage());
        }
        return build.getBuildNumber();
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
