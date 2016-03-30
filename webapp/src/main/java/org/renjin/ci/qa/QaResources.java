package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.*;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Path("/qa")
public class QaResources {
  
  private static Logger LOGGER = Logger.getLogger(QaResources.class.getName());

  
  @GET
  @Path("dashboard")
  public Viewable getDashboard() {

    Multimap<RenjinVersionId, RenjinVersionStat> statMap = HashMultimap.create();

    RenjinVersionStats stats = PackageDatabase.getRenjinVersionStats();
    if(stats != null) {
      for (RenjinVersionStat stat : stats.getVersions()) {
        statMap.put(stat.getRenjinVersionId(), stat);
      }
    }
    
    List<RenjinVersionSummary> versions = new ArrayList<>();
    for (RenjinVersionId renjinVersionId : statMap.keySet()) {
      versions.add(new RenjinVersionSummary(renjinVersionId, statMap.get(renjinVersionId)));
    }

    Collections.sort(versions, Ordering.natural().reverse());
    
    Map<String, Object> model = new HashMap<>();    
    model.put("versions", versions);
    
    return new Viewable("/dashboard.ftl", model);
  }
  
  @GET
  @Path("gcc-bridge")
  public Viewable getGccBridgeStatus() {
    QueryResultIterable<PackageVersion> versions = ofy().load()
        .type(PackageVersion.class)
        .filter("needsCompilation", true)
        .iterable();

    List<Key<PackageBuild>> buildsToFetch = new ArrayList<>();
    for (PackageVersion version : versions) {
      if(version.getLastBuildNumber() != 0) {
        buildsToFetch.add(PackageBuild.key(version.getPackageVersionId(), version.getLastBuildNumber()));
      }
    }

    Collection<PackageBuild> builds = ObjectifyService.ofy().load().keys(buildsToFetch).values();

    LOGGER.info("Build count = " + builds.size());
    
    Map<String, Object> model = new HashMap<>();
    model.put("builds", builds);
    
    return new Viewable("/gccBridge.ftl", model);
  }
  
  @GET
  @Path("progress/{renjinVersion}")
  public Viewable getDeltas(@PathParam("renjinVersion") String renjinVersion) {
    
    QueryResultIterable<PackageVersionDelta> deltas = ofy().load()
        .type(PackageVersionDelta.class)
        .filter("renjinVersions", renjinVersion)
        .iterable();

    List<DeltaViewModel> packages = new ArrayList<>();
    for (PackageVersionDelta delta : deltas) {
      Optional<BuildDelta> build = delta.getBuild(renjinVersion);
      if(build.isPresent()) {
        packages.add(new DeltaViewModel(delta.getPackageVersionId(), build.get()));
      }
    }
    
    Map<String, Object> model = new HashMap<>();
    model.put("renjinVersion", renjinVersion);
    model.put("packageVersions", packages);
    

    return new Viewable("/versionDeltas.ftl", model);
  }
  
  @GET
  @Path("testRegressions")
  public Viewable getTestRegressions() {

    Iterable<PackageVersionDelta> deltas = ofy().load().type(PackageVersionDelta.class)
        .filter("regression", true)
        .iterable();

    TestRegressionPage page = new TestRegressionPage(deltas);
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", page);
    
    return new Viewable("/testRegressions.ftl", model);
  }
  
  @GET
  @Path("stillFailing")
  public String getMostRecentFailingBuild(@QueryParam("pv") String packageVersion, 
                                          @QueryParam("renjinVersion") String renjinVersion,
                                          @QueryParam("test") String testName) {
    PackageVersionId packageVersionId = PackageVersionId.fromTriplet(packageVersion);
    Iterable<PackageTestResult> results = PackageDatabase.getTestResults(packageVersionId);
   
    // The version of Renjin with the first failure
    RenjinVersionId regressionVersion = RenjinVersionId.valueOf(renjinVersion);
    
    // The last version of Renjin still failing
    RenjinVersionId lastFailingVersion = regressionVersion;
    long lastFailingBuild = 0;
    for (PackageTestResult result : results) {
      if(result.getRenjinVersionId().compareTo(lastFailingVersion) > 0 && !result.isPassed()) {
        lastFailingVersion = result.getRenjinVersionId();
        lastFailingBuild = result.getPackageBuildNumber();
      }
    }
    if(lastFailingVersion.equals(regressionVersion)) {
      return "";
    } else {
      return "Still failing on " + lastFailingVersion + " <a href=\"" + 
          new PackageBuildId(packageVersionId, lastFailingBuild).getPath() + "\">#" + lastFailingBuild + "</a>";
    }
  }
}
