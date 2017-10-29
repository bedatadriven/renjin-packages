package org.renjin.ci.pulls;

import com.google.api.client.util.Lists;
import com.google.common.collect.HashMultimap;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PullPackageBuild;

import java.util.ArrayList;
import java.util.List;

public class PullRequestPage {

  private int number;
  private final List<PullPackageBuild> packageBuilds;
  private final List<BuildSection> builds = new ArrayList<>();

  public PullRequestPage(int number) {
    this.number = number;
    packageBuilds = Lists.newArrayList(PackageDatabase.getPullPackageBuilds(number));

    HashMultimap<String, PullPackageBuild> map = HashMultimap.create();

    for (PullPackageBuild packageBuild : packageBuilds) {
      String buildNumber = packageBuild.getPullBuildId().getBuildNumber();
      map.put(buildNumber, packageBuild);
    }

    for (String buildNumber : map.keySet()) {
      builds.add(new BuildSection(buildNumber, map.get(buildNumber)));
    }
  }

  public int getNumber() {
    return number;
  }

  public List<BuildSection> getBuilds() {
    return builds;
  }
}
