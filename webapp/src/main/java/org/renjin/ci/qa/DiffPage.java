package org.renjin.ci.qa;


import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.releases.ReleasesResource;

import java.io.IOException;
import java.util.*;

public class DiffPage {
  private RenjinRelease lastGood;
  private RenjinRelease broken;
  private final GHCompare compare;

  private List<String> filesChanged;

  public DiffPage(RenjinRelease lastGood, RenjinRelease broken, GHCompare compare) throws IOException {
    this.lastGood = lastGood;
    this.broken = broken;
    this.compare = compare;

    Set<String> fileSet = new HashSet<>();
    for (GHCompare.Commit commit : compare.getCommits()) {
      for (GHCommit.File file : commit.getFiles()) {
        fileSet.add(file.getFileName());
      }
    }

    filesChanged = new ArrayList<>(fileSet);
    Collections.sort(filesChanged);
  }

  public String getBadCommitId() {
    return broken.getCommitSha1();
  }

  public String getGoodCommitId() {
    return lastGood.getCommitSha1();
  }

  public List<GHCompare.Commit> getCommits() {
    return Arrays.asList(compare.getCommits());
  }

  public List<String> getFiles() {
    return filesChanged;
  }

  public String getGitHubCompareUrl() {
    return ReleasesResource.compareUrl(lastGood.getVersionId(), broken.getVersionId());
  }

}
