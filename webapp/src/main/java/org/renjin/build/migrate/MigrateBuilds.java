package org.renjin.build.migrate;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;
import org.renjin.build.model.Package;
import org.renjin.build.tasks.RegisterPackageVersionTask;

import javax.persistence.Tuple;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MigrateBuilds {

  public MigrateBuilds() {

  }

  @POST
  @Path("build")
  public Response migrateBuild(@FormParam("build") String packageId) {


    Objectify ofy = ObjectifyService.ofy();

    List<Tuple> rows = HibernateUtil
        .getActiveEntityManager()
        .createQuery("select b.packageVersion.id, b.build.id,  b.build.renjinCommit.version, b.outcome " +
            "from RPackageBuild  b " +
            "where b.packageVersion.rPackage.id = :packageId and b.stage = 'COMPLETED' " +
            "order by b.build.started", Tuple.class)
            .setParameter("packageId", packageId)
        .getResultList();

    List<PackageBuild> entities = Lists.newArrayList();
    Map<PackageVersionId, PackageStatus> statusMap = Maps.newHashMap();

    for(Tuple row : rows) {

      PackageVersionId pvid = new PackageVersionId(row.get(0, String.class));
      long buildNumber = row.get(1, Long.class);

      PackageBuild build = new PackageBuild(pvid, buildNumber);
      build.setRenjinVersion(row.get(2, String.class));
      build.setOutcome(row.get(1, BuildOutcome.class));
      entities.add(build);

      PackageStatus status = statusMap.get(pvid);
      if(status == null) {
        status = new PackageStatus(pvid, build.getRenjinVersionId());
        statusMap.put(pvid, status);
      }
      switch(build.getOutcome()) {
        case ERROR:
        case FAILURE:
        case TIMEOUT:
          if(status.getBuildStatus() != BuildStatus.BUILT) {
            status.setBuildStatus(BuildStatus.FAILED);
            status.setBuildNumber(build.getBuildNumber());
          }
          break;
        case SUCCESS:
          status.setBuildStatus(BuildStatus.BUILT);
          status.setBuildNumber(buildNumber);
          break;
      }

      if(entities.size() > 50) {
        ofy.save().entities(entities);
        entities.clear();
      }
    }

    if(!entities.isEmpty()) {
      ofy.save().entities(entities);
    }

    return Response.ok().build();
  }

  @POST
  @Path("package")
  public Response migrateVersions(@FormParam("packageId") String packageId) {


    Objectify ofy = ObjectifyService.ofy();

    List<Object> entities = Lists.newArrayList();

    RPackage pkg = HibernateUtil.getActiveEntityManager().find(RPackage.class, packageId);

    Package packageEntity = new Package();
    packageEntity.setId(pkg.getId());
    packageEntity.setLatestVersion(pkg.getLatestVersion().getId());
    packageEntity.setTitle(pkg.getTitle());
    entities.add(packageEntity);

    for(RPackageVersion v : pkg.getVersions()) {
      PackageVersion version = new PackageVersion();
      version.setId(v.getId());
      version.setDescription(v.getDescription());

//      if(v.getPublicationDate() != null) {
//        //version.setPublicationDate(new Date(v.getPublicationDate()));
//      }

      List<String> dependencies = Lists.newArrayList();

      for(RPackageDependency dep : v.getDependencies()) {
        dependencies.add(dep.getDependency().getId());
      }
      //version.setDependenciesFrom(dependencies);
      entities.add(version);
    }

    ofy.save().entities(entities);

    return Response.ok().build();
  }

  @POST
  @Path("enqueue")
  public Response migrate() {
//    List<Integer> builds = HibernateUtil
//        .getActiveEntityManager()
//        .createQuery("select distinct b.build.id " +
//            "from RPackageBuild  b where b.stage = 'COMPLETED'", Integer.class)
//        .getResultList();
//
//    for(Integer buildId : builds) {
//      QueueFactory.getDefaultQueue().add(TaskOptions.Builder
//          .withUrl("/migrateBuilds/build")
//          .param("build", Integer.toString(buildId)));
//    }

    List<String> pkgs = HibernateUtil
        .getActiveEntityManager()
        .createQuery("select b.id " +
            "from RPackageVersion  b", String.class)
        .getResultList();

    for(String packageId : pkgs) {
      RegisterPackageVersionTask.queue(new PackageVersionId(packageId));
    }

    return Response.ok().build();
  }

  @POST
  public String enqueueMigration() {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/migrateBuilds/enqueue"));
    return "OK!";
  }
}
