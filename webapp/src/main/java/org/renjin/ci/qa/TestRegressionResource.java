package org.renjin.ci.qa;

import com.googlecode.objectify.LoadResult;
import org.glassfish.jersey.server.mvc.Viewable;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.renjin.ci.GitHubFactory;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Details on a single test regression
 */
public class TestRegressionResource {
  private final PackageVersionId packageVersionId;
  private final PackageVersionDelta versionDelta;
  private final BuildDelta buildDelta;
  private String testName;
  private TestRegression regression;

  public TestRegressionResource(PackageVersionId packageVersionId, String testName) {
    this.packageVersionId = packageVersionId;
    this.versionDelta = PackageDatabase.getDelta(packageVersionId).now();
    this.buildDelta = findDelta(versionDelta, testName);
    this.testName = testName;
    this.regression = new TestRegression(versionDelta, buildDelta, testName);
  }

  private static BuildDelta findDelta(PackageVersionDelta delta, String testName) {
    for (BuildDelta buildDelta : delta.getBuilds()) {
      if(buildDelta.getTestRegressions().contains(testName)) {
        return buildDelta;
      }
    }
    return null;
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
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodRenjinVersion());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getBrokenRenjinVersionId());

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
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodRenjinVersion());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getBrokenRenjinVersionId());

    Map<String, Object> model = new HashMap<>();
    model.put("regression", regression);
    model.put("goodCommitId", lastGood.now().getCommitSha1());
    model.put("badCommitId", broken.now().getCommitSha1());

    return new Viewable("/testRegressionBisect.ftl", model);
  }

}
