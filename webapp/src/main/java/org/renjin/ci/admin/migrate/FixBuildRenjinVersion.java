package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import org.joda.time.DateTime;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.logging.Logger;

/**
 * Created by alex on 11-5-15.
 */
public class FixBuildRenjinVersion extends ForEachEntityAsBean<PackageBuild> {

  private static final Logger LOGGER = Logger.getLogger(FixBuildRenjinVersion.class.getName());
  
  public FixBuildRenjinVersion() {
    super(PackageBuild.class);
  }

  @Override
  public void apply(PackageBuild entity) {
    if(entity.getRenjinVersion() == null) {

      DateTime buildDate = new DateTime(entity.getStartTime());
      if(buildDate.getYear() == 2015 && buildDate.getMonthOfYear()==5 && buildDate.getDayOfMonth()==11) {
        entity.setRenjinVersion("0.7.1525");
        ObjectifyService.ofy().save().entity(entity);
      } else {

        throw new RuntimeException(entity.getId() + " has no renjinVersion but was built on " + buildDate);
      }
      
    }
  }
}
