package org.renjin.ci.pulls;

import com.google.api.client.util.Lists;
import org.renjin.ci.datastore.PullPackageBuild;

import java.util.List;
import java.util.Set;

public class BuildSection {


  private String buildNumber;
  private List<PullPackageBuild> packages;

  public BuildSection(String buildNumber, Set<PullPackageBuild> builds) {
    this.buildNumber = buildNumber;
    this.packages = Lists.newArrayList(builds);
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public List<PullPackageBuild> getPackages() {
    return packages;
  }
}
