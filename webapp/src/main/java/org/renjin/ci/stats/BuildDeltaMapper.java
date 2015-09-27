package org.renjin.ci.stats;


import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.model.DeltaType;

import java.util.logging.Logger;

/**
 * Maps a PackageVersionDelta -> [RenjinVersionId: +/-]
 */
public class BuildDeltaMapper extends Mapper<Entity, DeltaKey, DeltaValue> {
  
  private static final Logger LOGGER = Logger.getLogger(BuildDeltaMapper.class.getName());


  @Override
  public void map(Entity entity) {

    PackageVersionDelta delta = ObjectifyService.ofy().load().fromEntity(entity);

    if(delta.getBuilds() != null) {
      for (BuildDelta build : delta.getBuilds()) {
        if (build.getBuildDelta() < 0) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.BUILD), new DeltaValue(delta.getPackageId(), Deltas.REGRESSION));
        } else if (build.getBuildDelta() > 0) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.BUILD), new DeltaValue(delta.getPackageId(), Deltas.PROGRESSION));
        }
        if (build.getCompilationDelta() < 0) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.COMPILATION), new DeltaValue(delta.getPackageId(), Deltas.REGRESSION));
        } else if (build.getCompilationDelta() > 0) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.COMPILATION), new DeltaValue(delta.getPackageId(), Deltas.PROGRESSION));
        }

        for (String test : build.getTestRegressions()) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.TESTS), new DeltaValue(delta.getPackageId(), test, Deltas.REGRESSION));
        }

        for (String test : build.getTestProgressions()) {
          emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.TESTS), new DeltaValue(delta.getPackageId(), test, Deltas.PROGRESSION));
        }

      }
    }
  }
}
