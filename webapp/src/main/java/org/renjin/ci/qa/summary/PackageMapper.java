package org.renjin.ci.qa.summary;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.BuildStatus;
import org.renjin.ci.model.PackageStatus;
import org.renjin.ci.model.RenjinVersionId;

public class PackageMapper extends Mapper<Entity, RenjinVersionId, String> {

  @Override
  public void map(Entity value) {
    PackageStatus status = ObjectifyService.ofy().load().fromEntity(value);
    if(status.getBuildStatus() == BuildStatus.BUILT) {
      emit(status.getRenjinVersionId(), status.getPackageVersionId().getGroupId() + ":" + status.getPackageName());
    }
  }
}
