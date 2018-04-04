package org.renjin.ci.qa;

import com.googlecode.objectify.LoadResult;
import org.glassfish.jersey.server.mvc.Viewable;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.renjin.ci.GitHubFactory;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Details on a single test regression
 */
public class TestRegressionResource {
  private final PackageVersionId packageVersionId;
  private final PackageVersionDelta versionDelta;
  private String testName;
  private TestRegression regression;

  public TestRegressionResource(PackageVersionId packageVersionId, String testName) {
    this.packageVersionId = packageVersionId;
    this.versionDelta = PackageDatabase.getDelta(packageVersionId).now();
    this.testName = testName;
    this.regression = new TestRegression(versionDelta, testName);
  }


  @GET
  public Viewable getSummary() {

    Map<String, Object> model = new HashMap<>();
    model.put("regression", regression);

    return new Viewable("/testRegression.ftl", model);
  }

  @GET
  @Path("diff")
  public Viewable getDiff() throws IOException {
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodResult().getRenjinVersionId());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getBrokenResult().getRenjinVersionId());

    GitHub github = GitHubFactory.create();
    GHRepository repo = github.getRepository("bedatadriven/renjin");

    GHCompare compare = repo.getCompare(lastGood.now().getCommitSha1(), broken.now().getCommitSha1());

    Map<String, Object> model = new HashMap<>();
    model.put("regression", regression);
    model.put("diff", new DiffPage(lastGood.now(), broken.now(), compare));

    return new Viewable("/testRegressionDiff.ftl", model);
  }

  @GET
  @Path("bisect")
  public Viewable getBisectForm() throws IOException {
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodResult().getRenjinVersionId());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getBrokenResult().getRenjinVersionId());

    Map<String, Object> model = new HashMap<>();
    model.put("regression", regression);
    model.put("goodCommitId", lastGood.now().getCommitSha1());
    model.put("badCommitId", broken.now().getCommitSha1());

    return new Viewable("/testRegressionBisect.ftl", model);
  }

  @GET
  @Path("next")
  public Response nextRegression(@Context UriInfo uriInfo) {
    return Response.seeOther(
      QaResources.findNextRegression(uriInfo, packageVersionId, testName))
        .build();
  }

}
