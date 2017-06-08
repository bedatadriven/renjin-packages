package org.renjin.ci.source;

import com.google.common.annotations.VisibleForTesting;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GitHub;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Provides redirects to java source lookup
 */
public class JavaRedirects {

  private static final Logger LOGGER = Logger.getLogger(JavaRedirects.class.getName());

  @GET
  public Response redirect(@QueryParam("build") String buildId,
                           @QueryParam("method") String javaMethod,
                           @QueryParam("file") String file,
                           @QueryParam("line") int lineNumber) throws IOException, URISyntaxException {

    PackageBuild build = PackageDatabase.getBuild(PackageBuildId.fromString(buildId)).now();

    URL sourceUrl;
    if(javaMethod.startsWith("org.renjin.cran") || javaMethod.startsWith("org.renjin.bioconductor")) {
      sourceUrl = findPackageSource(build, javaMethod, file);
    } else if(javaMethod.startsWith("org.renjin")) {
      sourceUrl = findRenjinSource(build, javaMethod, file);
    } else {
      // Try sending to Jenkins...
      return Response.temporaryRedirect(
          new URI("http://stacktrace.jenkins-ci.org/search/?query=" + javaMethod + "&entity=method"))
          .build();
    }

    return Response.temporaryRedirect(UriBuilder.fromUri(sourceUrl.toURI()).fragment("L" + lineNumber).build()).build();
  }

  private URL findPackageSource(PackageBuild build, String javaMethod, String file) throws IOException {
    PackageId packageId = packageIdFromMethodName(javaMethod);
    if(packageId.equals(build.getPackageId())) {
      return findPackageSource(build.getPackageVersionId(), file);

    } else {
      return findPackageSourceInDependency(build, packageId, file);
    }
  }

  @VisibleForTesting
  static URL findPackageSource(PackageVersionId packageVersionId, String file) throws IOException {
    String packageRepo = packageRepo(packageVersionId);
    GHRef ref = GitHub.connectAnonymously().getRepository(packageRepo).getRef("tags/" + packageVersionId.getVersionString());
    return findFile(packageRepo(packageVersionId), ref.getObject().getSha(), file);
  }

  private static String packageRepo(PackageVersionId packageVersionId) {
    if(packageVersionId.getGroupId().equals("org.renjin.cran")) {
      return "cran/" + packageVersionId.getPackageName();
    } else {
      throw new UnsupportedOperationException("Github repo for :" + packageVersionId);
    }
  }

  private URL findPackageSourceInDependency(PackageBuild build, PackageId packageId, String file) throws IOException {
    for (PackageVersionId packageVersionId : build.getResolvedDependencyIds()) {
      if(packageVersionId.getPackageId().equals(packageId)) {
        return findPackageSource(packageVersionId, file);
      }
    }
    throw new UnsupportedOperationException("TODO");
  }

  @VisibleForTesting
  static PackageId packageIdFromMethodName(String javaMethod) {

    // Example inputs:
    // org.renjin.cran.lazyeval.lazy__.make_lazy_dots

    // First trim method name ('make_lazy_dots')
    int methodDelim = javaMethod.lastIndexOf('.');
    String className = javaMethod.substring(0, methodDelim);

    // Now trim the simple class name ('lazy__')
    int packageDelim = className.lastIndexOf('.');
    String javaPackage = className.substring(0, packageDelim);

    // Finally strip of the group id...
    String groupId = parseGroupIdFromJavaPackage(javaPackage);
    String packageName = javaPackage.substring(groupId.length() + ".".length());

    return new PackageId(groupId, packageName);
  }

  private static String parseGroupIdFromJavaPackage(String javaPackage) {
    if(javaPackage.startsWith("org.renjin.cran")) {
      return "org.renjin.cran";
    } else if(javaPackage.startsWith("org.renjin.bioconductor")) {
      return "org.renjin.bioconductor";
    } else {
      throw new UnsupportedOperationException("Can't parse group id from " + javaPackage);
    }
  }

  private URL findRenjinSource(PackageBuild build, String javaMethod, String fileName) throws IOException, URISyntaxException {

    LOGGER.info("Finding Renjin Source " + fileName + "...");

    RenjinRelease release = PackageDatabase.getRenjinRelease(build.getRenjinVersionId()).now();

    return findFile("bedatadriven/renjin", release.getCommitSha1(), fileName);

  }

  @VisibleForTesting
  static URL findFile(String repo, String sha1, String fileName) throws IOException {

    GitHubTreeList treeList = GitHubTreeList.fetch(repo, sha1);

    LOGGER.info("Fetched list of " + treeList.getPaths().size() + " paths from " + repo + "@" + sha1);


    for (String path : treeList.getPaths()) {
      String baseName = baseName(path);
      if(baseName.equals(fileName)) {
        return new URL(String.format("https://github.com/%s/blob/%s/%s", repo, sha1, path));
      }
    }

    throw new RuntimeException("Could not find file " + fileName);
  }

  private static String baseName(String path) {
    int lastSlash = path.lastIndexOf('/');
    return path.substring(lastSlash+1);
  }


}
