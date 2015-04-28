package org.renjin.ci.qa.stats;


import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.PackageBuild;

import java.util.logging.Logger;

/**
 * Maps a PackageBuild -> [RenjinVersionId: +/-]
 */
public class BuildDeltaMapper extends Mapper<Entity, String, Integer> {
  
  private static final Logger LOGGER = Logger.getLogger(BuildDeltaMapper.class.getName());
  
  @Override
  public void map(Entity entity) {
    if(entity.getParent() == null) {
      LOGGER.severe("Malformed PackageBuild key: " + entity.getKey());
      return;
    }

    PackageBuild build = ObjectifyService.ofy().load().fromEntity(entity);
    if(build.getRenjinVersion() == null) {
      LOGGER.severe("PackageBuild " + entity.getKey() + " is missing RenjinVersion");
      return;
    }
    
    if(build.getBuildDelta() < 0) {
      emit(build.getRenjinVersion(), Deltas.BUILD_REGRESSION);
    } else if(build.getBuildDelta() > 0) {
      emit(build.getRenjinVersion(), Deltas.BUILD_PROGRESSION);
    }
  }
}
