package org.renjin.ci.stats;


import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.model.DeltaType;

import java.util.logging.Logger;

/**
 * Maps a PackageBuild -> [RenjinVersionId: +/-]
 */
public class BuildDeltaMapper extends Mapper<Entity, DeltaKey, DeltaValue> {
  
  private static final Logger LOGGER = Logger.getLogger(BuildDeltaMapper.class.getName());


  @Override
  public void map(Entity entity) {
    if(entity.getParent() == null) {
      LOGGER.severe("Malformed PackageBuild key: " + entity.getKey());
      return;
    }

    PackageBuild build = PackageDatabase.load().fromEntity(entity);
    
    if(build.getRenjinVersion() == null) {
      LOGGER.severe("PackageBuild " + entity.getKey() + " is missing RenjinVersion");
      return;
    }
    
    if(build.getBuildDelta() < 0) {
      emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.BUILD), new DeltaValue(build.getPackageId(), Deltas.REGRESSION));
    } else if(build.getBuildDelta() > 0) {
      emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.BUILD), new DeltaValue(build.getPackageId(), Deltas.PROGRESSION));
    }
    
    if(build.getCompilationDelta() < 0) {
      emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.COMPILATION), new DeltaValue(build.getPackageId(), Deltas.REGRESSION));
    } else if(build.getCompilationDelta() > 0) {
      emit(new DeltaKey(build.getRenjinVersionId(), DeltaType.COMPILATION), new DeltaValue(build.getPackageId(), Deltas.PROGRESSION));
    }
  }
}
