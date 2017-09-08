package org.renjin.ci.pulls;

import com.google.api.client.util.Lists;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PullPackageBuild;

import java.util.List;

public class PullRequestPage {

  private int number;
  private final List<PullPackageBuild> packageBuilds;

  public PullRequestPage(int number) {
    this.number = number;
    packageBuilds = Lists.newArrayList(PackageDatabase.getPullPackageBuilds(number));
  }

  public int getNumber() {
    return number;
  }


  public List<PullPackageBuild> getBuilds() {
    return packageBuilds;
  }


}
