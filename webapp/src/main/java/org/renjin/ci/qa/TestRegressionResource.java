package org.renjin.ci.qa;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.VoidWork;
import org.glassfish.jersey.server.mvc.Viewable;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.renjin.ci.GitHubFactory;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.datastore.TestRegression;
import org.renjin.ci.datastore.TestRegressionStatus;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Details on a single test regression
 */
public class TestRegressionResource {
  private final TestRegression regression;


  public TestRegressionResource(TestRegression regression) {
    this.regression = regression;
  }


  @GET
  public Viewable getDetail() {

    Map<String, Object> model = new HashMap<>();
    model.put("regression", new TestRegressionPage(regression));

    return new Viewable("/testRegression.ftl", model);
  }

  @GET
  @Path("diff")
  public Viewable getDiff() throws IOException {
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodRenjinVersionId());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getRenjinVersionId());

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
  public Viewable getBisectForm() {
    LoadResult<RenjinRelease> lastGood = PackageDatabase.getRenjinRelease(regression.getLastGoodRenjinVersionId());
    LoadResult<RenjinRelease> broken = PackageDatabase.getRenjinRelease(regression.getRenjinVersionId());

    Map<String, Object> model = new HashMap<>();
    model.put("regression", regression);
    model.put("goodCommitId", lastGood.now().getCommitSha1());
    model.put("badCommitId", broken.now().getCommitSha1());

    return new Viewable("/testRegressionBisect.ftl", model);
  }

  @POST
  @Path("update")
  public Response update(@Context UriInfo uriInfo,
                         @FormParam("status") final String status,
                         @FormParam("summary") final String summary) {

    PackageDatabase.ofy().transact(new VoidWork() {
      @Override
      public void vrun() {
        TestRegression toUpdate = PackageDatabase.ofy().load().key(regression.getKey()).now();
        toUpdate.setSummary(summary);
        toUpdate.setStatus(TestRegressionStatus.valueOf(status));
        toUpdate.setTriageIndex(false);
        PackageDatabase.ofy().save().entity(toUpdate).now();
      }
    });

    return nextRegression(uriInfo);
  }

  @GET
  @Path("next")
  public Response nextRegression(@Context UriInfo uriInfo) {
    return Response.seeOther(nextUnconfirmedRegression(uriInfo)).build();
  }

  private URI nextUnconfirmedRegression(UriInfo uriInfo) {
    LoadResult<Key<TestRegression>> nextKey = PackageDatabase.getTestRegressions()
        .order("triage")
        .filter("triage >", regression.getTriageOrder())
        .keys()
        .first();

    if(nextKey.now() == null) {
      // If this is the last test, then
      // Redirect to history page
      return uriInfo.getBaseUriBuilder()
          .path("qa")
          .path("testRegressions")
          .build();

    } else {
      // Redirect to this test's page
      return uriInfo.getBaseUriBuilder()
          .path(TestRegression.idOf(nextKey.now()).getPath())
          .build();
    }
  }

}
