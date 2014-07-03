package org.renjin.ci.build;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.pipelines.EntityMapFunction;
import org.renjin.ci.tasks.PackageCheckQueue;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Resets Package Status
 */
public class ResetPackageStatus extends EntityMapFunction<PackageVersion> {

  public ResetPackageStatus() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion packageVersion) {
    PackageCheckQueue.createStatus(packageVersion, RenjinVersionId.RELEASE);
  }
}
