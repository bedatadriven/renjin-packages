package org.renjin.ci.packages;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.renjin.ci.model.PackageStatus;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackageViewModel {

  private String groupId;
  private String name;
  private List<PackageStatus> status;

  public PackageViewModel(String groupId, String name) {
    this.groupId = groupId;
    this.name = name;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getName() {
    return name;
  }

  public List<PackageStatus> getStatus() {
    return status;
  }

  public void setStatus(List<PackageStatus> status) {
    this.status = status;
  }

  public Set<PackageVersionId> getVersions() {
    Set<PackageVersionId> set = Sets.newHashSet();
    for(PackageStatus status : this.status) {
      set.add(status.getPackageVersionId());
    }
    return set;
  }

  public PackageVersionId getLatestVersion() {
    return Collections.max(getVersions());
  }

  public Map<RenjinVersionId, PackageStatus> statusOf(PackageVersionId version) {
    Map<RenjinVersionId, PackageStatus> map = Maps.newHashMap();
    for(PackageStatus status : this.status) {
      if(status.getRenjinVersionId().equals(version)) {
        map.put(status.getRenjinVersionId(), status);
      }
    }
    return map;
  }

}
