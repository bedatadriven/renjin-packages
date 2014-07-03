package org.renjin.ci.build;


import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.PackageBuild;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.pipelines.EntityMapFunction;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class TimeoutAllBuilds extends EntityMapFunction<PackageBuild> {

  public TimeoutAllBuilds() {
    super(PackageBuild.class);
  }

  @Override
  public void apply(PackageBuild build) {

    if(build.getOutcome() == null) {
      build.setEndTime(System.currentTimeMillis());
      build.setStartTime(null);
      build.setOutcome(BuildOutcome.TIMEOUT);
      ofy().save().entity(build).now();
    }
  }
}
