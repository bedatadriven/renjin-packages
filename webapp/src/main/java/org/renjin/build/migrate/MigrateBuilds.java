package org.renjin.build.migrate;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;
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

public class MigrateBuilds {

  public MigrateBuilds() {

  }

  @POST
  @Path("build")
  public Response migrateBuild(@FormParam("build") int buildId) {


    Objectify ofy = ObjectifyService.ofy();

    List<Tuple> builds = HibernateUtil
        .getActiveEntityManager()
        .createQuery("select b.id, b.outcome, b.build.renjinCommit.version " +
            "from RPackageBuild  b where b.build.id = :build and b.stage = 'COMPLETED'", Tuple.class)
            .setParameter("build", buildId)
        .getResultList();

    List<PackageBuild> entities = Lists.newArrayList();

    for(Tuple row : builds) {

      PackageBuild entity = new PackageBuild();
      entity.setId(row.get(0, String.class));
      entity.setOutcome(row.get(1, BuildOutcome.class));
      entity.setRenjinVersion(row.get(2, String.class));
      entity.setStage(BuildStage.COMPLETED);
      entities.add(entity);

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
      //version.setDependencies(dependencies);
      entities.add(version);
    }

    ofy.save().entities(entities);

    return Response.ok().build();
  }

  @POST
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
}
