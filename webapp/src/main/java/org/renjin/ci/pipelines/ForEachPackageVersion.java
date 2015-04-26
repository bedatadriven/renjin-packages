package org.renjin.ci.pipelines;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.mapreduce.MapOnlyMapper;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

public abstract class ForEachPackageVersion extends MapOnlyMapper<Key, Void> {
  
  
  @Override
  public final void map(Key value) {
    PackageId packageId = PackageId.valueOf(value.getParent().getName());
    String version = value.getName();
    apply(new PackageVersionId(packageId, version));
  }
  
  public String getJobName() {
    return getClass().getSimpleName();
  }

  protected abstract void apply(PackageVersionId packageVersionId);
}
