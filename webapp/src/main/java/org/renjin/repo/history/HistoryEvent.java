package org.renjin.repo.history;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.renjin.repo.model.RenjinCommit;
import org.renjin.repo.model.TestResult;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HistoryEvent {

  private final Date time;
  private final String commitId;
  private final String release;

  private String changeMessage;
  private boolean passing;

  private final List<TestResult> testResults;


  public HistoryEvent(RenjinCommit head, Collection<TestResult> testResults) {
    this.commitId = head.getId();
    this.time = head.getCommitTime();
    if(!head.getVersion().endsWith("-SNAPSHOT")) {
      this.release = head.getVersion();
    } else {
      this.release = null;
      this.changeMessage = head.getMessage();
    }

    this.testResults = Lists.newArrayList(testResults);

  }

  public Date getTime() {
    return time;
  }


  public String getCommitId() {
    return commitId;
  }

  public String getAbbreviatedCommitId() {
    return commitId.substring(0, 7);
  }

  public String getRelease() {
    return release;
  }

  public List<TestResult> getTestResults() {
    return testResults;
  }

  public String getChangeMessage() {
    return changeMessage;
  }

  public String getChangeSummary() {
    if(changeMessage == null) {
      return null;
    } else {
      return changeMessage.split("\n")[0];
    }
  }

  public void setChangeMessage(String changeMessage) {
    this.changeMessage = changeMessage;
  }

  public void setPassing(boolean passing) {
    this.passing = passing;
  }

  public boolean isPassing() {
    return passing;
  }

  public boolean hasTestResults() {
    return !testResults.isEmpty();
  }

  public TestResult getLatestTestResult() {
    if(testResults.isEmpty()) {
      return null;
    } else {
      return testResults.get(0);
    }
  }

  public List<TestResult> getOldTestResults() {
    if(testResults.size() <= 1) {
      return Collections.emptyList();
    } else {
      return testResults.subList(1, testResults.size());
    }
  }
}
