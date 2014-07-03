package org.renjin.ci;

import com.google.common.collect.Lists;
import org.renjin.ci.model.RenjinCommit;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CommitViewModel {
  private RenjinCommit commit;
  private List<BuildViewModel> builds;

  public CommitViewModel(RenjinCommit commit, Collection<BuildViewModel> buildViewModels) {
    this.commit = commit;
    this.builds = Lists.newArrayList(buildViewModels);
    Collections.sort(this.builds);
  }

  public List<BuildViewModel> getBuilds() {
    return builds;
  }

  public String getId() {
    return commit.getId();
  }

  public Date getCommitTime() {
    return commit.getCommitTime();
  }

  public String getAbbreviatedId() {
    return commit.getAbbreviatedId();
  }

  public String getVersion() {
    return commit.getVersion();
  }

  public String getTopLine() {
    return commit.getTopLine();
  }

}
