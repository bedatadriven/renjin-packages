package org.renjin.ci;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.model.*;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RenjinCiClient {

  private static final Logger LOGGER = Logger.getLogger(RenjinCiClient.class.getName());

  public static final String ROOT_URL = "https://10-dot-packages-dot-renjinci.appspot.com";


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
  
  public static List<PackageVersionId> resolveDependencies(List<PackageDependency> dependencies) {
    
    if(dependencies.isEmpty()) {
      return Collections.emptyList();
    }
    
    WebTarget path = rootTarget()
        .path("packages")
        .path("resolveDependencies");

    for (PackageDependency dependency : dependencies) {
      path = path.queryParam(dependency.getName(), dependency.getVersion());
    }
    
    ArrayNode versions = path.request().get(ArrayNode.class);

    List<PackageVersionId> versionIds = new ArrayList<>();
    for (JsonNode version : versions) {
      versionIds.add(PackageVersionId.fromTriplet(version.asText()));
    }
    return versionIds;
  }

  public static ResolvedDependencySet resolveSuggests(final List<PackageDependency> dependencies) {

    return withRetries(new Callable<ResolvedDependencySet>() {
      @Override
      public ResolvedDependencySet call() throws Exception {
        WebTarget target = client().target(ROOT_URL)
            .path("packages")
            .path("resolveSuggests");

        for (PackageDependency dependency : dependencies) {
          target = target.queryParam("p", dependency.getName());
        }

        return target.request().get(ResolvedDependencySet.class);
      }
    });
  }
  
  public static ListenableFuture<ResolvedDependencySet> resolveDependencies(ListeningExecutorService service, 
                                                                            final PackageVersionId id) {
    return service.submit(new Callable<ResolvedDependencySet>() {
      @Override
      public ResolvedDependencySet call() throws Exception {
        return resolveDependencies(id);
      }
    });
  }
  
  public static PackageBuildId queryLastSuccessfulBuild(PackageVersionId packageVersionId) {
    String buildVersion = packageVersion(packageVersionId)
        .path("lastSuccessfulBuild")
        .request()
        .get(String.class);
    
    return new PackageBuildId(packageVersionId, buildVersion);
  }


  public static long postBuild(PackageVersionId packageVersionId, String renjinVersion) {
    Preconditions.checkNotNull(renjinVersion, "renjinVersion cannot be null");

    PackageBuild build;

    Form form = new Form();
    form.param("renjinVersion", renjinVersion);

    WebTarget builds = packageVersion(packageVersionId).path("builds");
    try {

      build = builds.request(MediaType.APPLICATION_JSON)
          .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), PackageBuild.class);


    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception getting build number", e);
      throw new RuntimeException("Failed to get next build number from " + builds.getUri() + ": " + e.getMessage());
    }
    return build.getBuildNumber();
  }


  public static void postPullRequestBuild(long pullNumber, int buildNumber, String commitId) {
    Form form = new Form();
    form.param("buildNumber", Integer.toString(buildNumber));
    form.param("commitId", commitId);

    rootTarget()
        .path("pull")
        .path(Long.toString(pullNumber))
        .path("build")
        .request()
        .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
  }

  private static WebTarget packageVersion(PackageVersionId packageVersionId) {
    return rootTarget()
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString());
  }


  public static void postResult(final PackageBuild build, final PackageBuildResult result) {
    final PackageVersionId packageVersionId = build.getPackageVersionId();

    withRetries(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        WebTarget buildResource = client().target(ROOT_URL)
            .path("package")
            .path(packageVersionId.getGroupId())
            .path(packageVersionId.getPackageName())
            .path(packageVersionId.getVersionString())
            .path("build")
            .path(Long.toString(build.getBuildNumber()));

        Response response = buildResource.request().post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));

        if(response.getStatus() != 200) {
          throw new RuntimeException("Failed to publish results: " + response.getStatus());
        }

        return null;
      }
    });
  }


  public static void postResult(final PullBuildId pullBuild, final PackageBuildResult result) {
    withRetries(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        WebTarget buildResource = client().target(ROOT_URL)
            .path("pull")
            .path(Long.toString(pullBuild.getPullNumber()))
            .path("build")
            .path(Long.toString(pullBuild.getPullBuildNumber()))
            .path("packageBuild");

        buildResource.request().post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));

        return null;
      }
    });
  }


  public static void postTestResults(PackageVersionId packageVersionId, List<TestResult> results) {
    WebTarget versionResource = client().target(ROOT_URL)
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString())
        .path("examples/results");

    versionResource
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(results, MediaType.APPLICATION_JSON_TYPE));
  }
  
  public static List<PackageVersionId> queryPackageList(String filter) {
    
    String url = ROOT_URL + "/packages/" + filter;
    String[] ids = client().target(url).request().get(String[].class);

    List<PackageVersionId> packageVersionIds = new ArrayList<PackageVersionId>();
    for (String id : ids) {
      packageVersionIds.add(PackageVersionId.fromTriplet(id));
    }
    return packageVersionIds;
  }


  public static List<TestResult> getTestResults(PackageBuildId buildId) {
    return client().target(ROOT_URL)
        .path("package")
        .path(buildId.getGroupId())
        .path(buildId.getPackageName())
        .path(buildId.getPackageVersionId().getVersionString())
        .path("build")
        .path(Long.toString(buildId.getBuildNumber()))
        .path("testResults")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(new GenericType<List<TestResult>>(){});
  }

  public static RenjinVersionId getLatestRenjinRelease() {
    String version = client().target(ROOT_URL)
        .path("releases")
        .path("latest")
        .request(MediaType.TEXT_PLAIN)
        .get(String.class);
    return RenjinVersionId.valueOf(version);
  }
  
  public static List<RenjinVersionId> getRenjinVersionRange(String from, String to) {
    ArrayNode array = client().target(ROOT_URL)
        .path("releases")
        .queryParam("from", from)
        .queryParam("to", to)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(ArrayNode.class);
    
    List<RenjinVersionId> versionIds = Lists.newArrayList();
    for (JsonNode release : array) {
      String version = release.get("version").asText();
      versionIds.add(RenjinVersionId.valueOf(version));
    }
    
    return versionIds;
  }

  public static void postRenjinRelease(String renjinVersion, String commitId) {
    
    // assume last part of version is build number
    String versionParts[] = renjinVersion.split("\\.");
    String buildNumber = versionParts[versionParts.length-1];
    
    Form form = new Form();
    form.param("renjinVersion", renjinVersion);
    form.param("sha1", commitId);
    form.param("buildNumber", buildNumber);

    client().target(ROOT_URL).path("releases").request().post(Entity.form(form));
  }

  public static void registerArtifacts(String renjinVersion, List<String> objectNames) {
    Form form = new Form();
    form.param("renjinVersion", renjinVersion);
    for (String objectName : objectNames) {
      form.param("objectName", objectName);
    }

    client().target(ROOT_URL).path("m2").request().post(Entity.form(form));
  }

  public static void postReplacementRelease(final String groupId, final String artifactId, String version) {
    final Form form = new Form();
    form.param("version", version);

    withRetries(new Callable<Void>() {
      @Override
      public Void call() {
        client().target(ROOT_URL)
            .path("package").path(groupId).path(artifactId).path("replacement")
            .request()
            .post(Entity.form(form));

        return null;
      }
    });

  }

  public static void postSystemRequirement(String name, String version) {
    Form form = new Form();
    form.param("name", name);
    form.param("version", version);

    Response response = client().target(ROOT_URL).path("systemRequirement")
        .request()
        .post(Entity.form(form));

    if(response.getStatus() != 200) {
      throw new RuntimeException("Failed: " + response.getStatus());
    }
  }

  public static Optional<String> getSystemRequirementVersion(String name) {
    Response response = client().target(ROOT_URL).path("systemRequirement")
        .path(name)
        .path("version")
        .request()
        .get();

    if(response.getStatus() == 404) {
      return Optional.absent();
    } else if(response.getStatus() == 200) {
      return Optional.of(response.readEntity(String.class));
    } else {
      throw new RuntimeException("Failed to retrieve latest version of system requirement " +
          name);
    }
  }

  public static long startBenchmarkRun(final BenchmarkRunDescriptor descriptor) {
    return withRetries(new Callable<Long>() {
      @Override
      public Long call() {
        return client().target(ROOT_URL)
            .path("benchmarks/runs")
            .request()
            .post(Entity.entity(descriptor, MediaType.APPLICATION_JSON_TYPE), Long.class);
      }
    });
  }
  
  public static void postBenchmarkResult(final long runId, String name, long milliseconds) {
    
    final Form form = new Form();
    form.param("name", name);
    form.param("runTime", Long.toString(milliseconds));
    form.param("completed", "true");

    withRetries(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        client().target(ROOT_URL)
            .path("benchmarks")
            .path("run")
            .path(Long.toString(runId))
            .path("results")
            .request()
            .post(Entity.form(form));
        return null;
      }
    });
  }

  public static void postBenchmarkFailure(final long runId, String name) {

    final Form form = new Form();
    form.param("name", name);
    form.param("completed", "false");

    withRetries(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        client().target(ROOT_URL)
            .path("benchmarks")
            .path("run")
            .path(Long.toString(runId))
            .path("results")
            .request()
            .post(Entity.form(form));
        return null;
      }
    });
  }

  private static <T> T withRetries(Callable<T> callable) {
    int retries = 8;
    while(true) {
      try {
        return callable.call();
      } catch (InternalServerErrorException e) {
        if (retries <= 0) {
          throw e;
        }
        retries--;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  public static void scheduleStatsUpdate() {
    Response response = client().target(ROOT_URL)
        .path("stats")
        .path("scheduleCountUpdate")
        .request()
        .buildPost(Entity.text(""))
        .invoke();
    
    LOGGER.info("scheduleStatsUpdate: " + response.getStatus());

  }

  public static String getPatchedVersionId(PackageVersionId pvid) throws IOException {

    // Avoiding hitting the API to check whether the branch exists in order to avoid rate limits
    Response head = client()
        .target(String.format("https://github.com/bedatadriven/%s.%s/tree/patched-%s",
            pvid.getGroupId(),
            pvid.getPackageName(),
            pvid.getVersionString()))
        .request()
        .head();

    if(head.getStatus() == 404) {
      return null;
    }

    Response response = client()
        .target(String.format("https://api.github.com/repos/bedatadriven/%s.%s/branches/patched-%s",
            pvid.getGroupId(),
            pvid.getPackageName(),
            pvid.getVersionString()))
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();

    if(response.getStatus() == 404) {
      return null;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode root = (ObjectNode) objectMapper.readTree(response.readEntity(String.class));
    ObjectNode commit = (ObjectNode) root.get("commit");
    return commit.get("sha").asText();
  }

  public static URL getPatchedVersionUrl(PackageVersionId pvid) {
    try {
      return new URL(String.format("https://github.com/bedatadriven/%s.%s/archive/patched-%s.zip",
          pvid.getGroupId(),
          pvid.getPackageName(),
          pvid.getVersionString()));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getPatchedArchiveFileName(PackageVersionId pvid) {
    return "patched-" + pvid.getVersionString() + ".zip";
  }

}
