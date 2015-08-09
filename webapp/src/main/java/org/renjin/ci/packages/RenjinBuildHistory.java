package org.renjin.ci.packages;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.RenjinVersionId;

import java.util.List;

public class RenjinBuildHistory {
  
  private RenjinVersionId renjinVersionId;
  private List<PackageBuild> builds;

  public RenjinBuildHistory(RenjinVersionId renjinVersionId, Iterable<PackageBuild> builds) {
    this.renjinVersionId = renjinVersionId;
    this.builds = Lists.newArrayList(builds);
  }

  public String getLabel() {
    return renjinVersionId.toString();
  }

  public List<PackageBuild> getBuilds() {
    return builds;
  }
}
