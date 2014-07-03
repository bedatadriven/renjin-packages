package org.renjin.ci.migrate;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.HibernateUtil;
import org.renjin.ci.PersistenceUtil;
import org.renjin.ci.model.*;
import org.renjin.ci.model.Package;
import org.renjin.ci.tasks.RegisterPackageVersionTask;

import javax.persistence.Tuple;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Path("/migrate")
public class MigrateBuilds {

  public MigrateBuilds() {

  }

  @POST
  @Path("build")
  public Response migrateBuild(@FormParam("build") String packageId) {

    Objectify ofy = ObjectifyService.ofy();

    List<Tuple> rows = HibernateUtil.getActiveEntityManager()
        .createQuery("select b.packageVersion.id, b.build.id,  b.build.renjinCommit.version, b.outcome, b.build.started," +
            "(b.packageVersion.loc.c + b.packageVersion.loc.fortran + b.packageVersion.cpp) as nativeCode, " +
            "b.nativeSourceCompilationFailure" +
            "from RPackageBuild  b " +
            "where b.packageVersion.rPackage.id = :packageId and b.stage = 'COMPLETED' " +
                " and b.build.renjinCommit.version in ('0.7.0-RC5', '0.7.0-RC6') " +
            "order by b.build.started", Tuple.class)
            .setParameter("packageId", packageId)
        .getResultList();

    List<PackageBuild> builds = Lists.newArrayList();
    Map<PackageVersionId, PackageStatus> statusMap = Maps.newHashMap();

    for(Tuple row : rows) {

      PackageVersionId pvid = new PackageVersionId(row.get(0, String.class));
      long buildNumber = row.get(1, Integer.class);

      PackageBuild build = new PackageBuild(pvid, buildNumber);
      build.setRenjinVersion(row.get(2, String.class));
      build.setOutcome(row.get(3, BuildOutcome.class));

      int nativeCodeLoc = row.get(4, Integer.class);
      if(nativeCodeLoc == 0) {
        build.setNativeOutcome(NativeOutcome.NA);
      } else {
        boolean failed = row.get(5, Boolean.class);
        build.setNativeOutcome(failed ? NativeOutcome.FAILURE : NativeOutcome.SUCCESS);
      }

      Date startTime = row.get(4, Date.class);
      if(startTime != null) {
        build.setStartTime(startTime.getTime());
      }
      builds.add(build);

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

      if(builds.size() > 50) {
        ofy.save().entities(builds);
        builds.clear();
      }
    }

    if(!builds.isEmpty()) {
      ofy.save().entities(Iterables.concat(builds, statusMap.values())).now();
    }

    return Response.ok().build();
  }

  @POST
  @Path("enqueue")
  public Response migrate() {
    List<String> packages = HibernateUtil
        .getActiveEntityManager()
        .createQuery("select distinct b.packageVersion.rPackage.id " +
            "from RPackageBuild b where b.stage = 'COMPLETED'", String.class)
        .getResultList();

    for(String packageId : packages) {
      QueueFactory.getDefaultQueue().add(TaskOptions.Builder
          .withUrl("/migrate/build")
          .param("build", packageId));
    }
//
//    List<String> pkgs = HibernateUtil
//        .getActiveEntityManager()
//        .createQuery("select b.id " +
//            "from RPackageVersion  b", String.class)
//        .getResultList();
//
//    for(String packageId : pkgs) {
//      RegisterPackageVersionTask.queue(new PackageVersionId(packageId));
//    }

    return Response.ok().build();
  }

  @POST
  public String enqueueMigration() {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/migrate/enqueue"));
    return "OK!";
  }
}
