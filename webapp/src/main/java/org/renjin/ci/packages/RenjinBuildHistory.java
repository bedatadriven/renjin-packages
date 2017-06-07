package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.RenjinVersionId;

import java.util.List;

public class RenjinBuildHistory {
  
  private String label;
  private int groupId;
  private boolean visible;
  private List<PackageBuild> builds;

  public RenjinBuildHistory(RenjinVersionId renjinVersionId, Iterable<PackageBuild> builds) {
    this.label = renjinVersionId.toString();
    this.builds = Lists.newArrayList(builds);
  }

  public int getGroupId() {
    return groupId;
  }

  public void setGroupId(int groupId) {
    this.groupId = groupId;
  }

  public void setBuilds(List<PackageBuild> builds) {
    this.builds = builds;
  }

  public String getLabel() {
    return label;
  }

  public List<PackageBuild> getBuilds() {
    return builds;
  }
}
