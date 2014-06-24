package org.renjin.build.queue;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Schedules a set of package build tasks
 */
public class BuildLauncher {


  private static final Logger LOGGER = Logger.getLogger(BuildLauncher.class.getName());

  @POST
  public Response startBuild(
                           @Context UriInfo uri,
                           @FormParam("renjinCommitId") String renjinCommitId) {

    // It takes quite awhile to schedule all 10k packages, so queue this as a longer-running task
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder
        .withUrl("/queue/launch/all")
        .param("renjinCommitId", renjinCommitId));

    return Response.seeOther(uri.getRequestUriBuilder().replacePath("/queue/dashboard").build()).build();
  }

  @POST
  @Path("all")
  public void launch(@FormParam("renjinVersion") String renjinVersion) {

    Objectify ofy = ObjectifyService.ofy();

    QueryResultIterator<Key<PackageVersion>> iterator = ofy.load()
        .type(PackageVersion.class)
        .filter("lastSuccessfulBuild = ", 0)
        .keys()
        .iterator();

    while(iterator.hasNext()) {

      QueueFactory.getDefaultQueue().add(TaskOptions.Builder
          .withUrl("/queue/launch/package")
          .param("renjinVersion", renjinVersion)
          .param("packageVersionId", iterator.next().getName()));

    }
  }

  @POST
  @Path("package")
  public Response launch(@FormParam("renjinVersion") String renjinVersion,
                         @FormParam("packageVersionId") String packageVersionId) {

    // is there another build queued for this package version?
    Objectify ofy = ObjectifyService.ofy();

    if(inProgress(packageVersionId, null)) {
      Response.ok().build();
    }

    // create the new build record
    PackageBuild build = new PackageBuild();
    build.setId(packageVersionId + "-b1000");
    build.setStage(BuildStage.WAITING);
    build.setRenjinVersion(renjinVersion);


    // determine which dependencies we need
    PackageVersion packageVersion = ofy.load()
        .type(PackageVersion.class)
        .filterKey(packageVersionId)
        .first()
        .now();

//
//    Set<String> dependencies = Sets.newHashSet();
//    Set<String> blocking = Sets.newHashSet();
//
    List<Key<PackageVersion>> dependencies = Lists.newArrayList();
    for(String dependencyId : packageVersion.getDependencies()) {
      dependencies.add(Key.create(PackageVersion.class, dependencyId));
    }
    Map<Key<PackageVersion>, PackageVersion> keys = ofy.load().keys(dependencies);


//    List<PackageVersion> dependencies = ofy.load().key
//
//    for(String dependency : packageVersion.getDependencies()) {
//      dependencies.add(dependency);
//
//
//    }
//
//    Iterable<PackageDescription.PackageDependency> declaredDeps =
//        Iterables.concat(description.getImports(), description.getDepends());
//
//    for(PackageDescription.PackageDependency dep : declaredDeps) {
//
//    //  PackageVersion latestVersion = findLatestVersion(ofy, dep);
//
//    }
    throw new UnsupportedOperationException();
  }


  private boolean inProgress(String packageVersionId, Objectify ofy) {

    QueryResultIterator<PackageBuild> iterator = ofy.load().type(PackageBuild.class)
        .filterKey(">=", packageVersionId)
        .iterator();

    while(iterator.hasNext()) {
      PackageBuild build = iterator.next();
      if(!build.getPackageVersionId().equals(packageVersionId)) {
        return false;
      }
      if(!build.isComplete()) {
        return true;
      }
    }
    return false;
  }


  private void scheduleBuild(StatelessSession session, Build build, String packageId,
                             Map<String, String> successfulBuildIds, List<String> dependencies) {
    if(packageId != null && !successfulBuildIds.containsKey(packageId)) {
      RPackageVersion version = new RPackageVersion();
      version.setId(packageId);

      RPackageBuild packageBuild = new RPackageBuild();
      packageBuild.setId(packageId + "-b" + build.getId());
      packageBuild.setBuild(build);
      packageBuild.setPackageVersion(version);

      if(dependencies.isEmpty()) {
        packageBuild.setStage(BuildStage.READY);
      } else {
        packageBuild.setDependencyVersions(Joiner.on(",").join(dependencies));
        if(successfulBuildIds.values().containsAll(dependencies)) {
          packageBuild.setStage(BuildStage.READY);
        } else {
          packageBuild.setStage(BuildStage.WAITING);
        }
      }
      session.insert(packageBuild);
    }
  }

  public static void main(String[] args) {
    new BuildLauncher().launch("cbf6435939168a0a527bdf97580bdb1f3f2ea264");
  }
}
