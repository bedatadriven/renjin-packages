package org.renjin.ci.packages;

import java.util.Collections;
import java.util.List;

public class RenjinBuildHistoryGroup {

  private List<RenjinBuildHistory> versions;
  private boolean visible;

  public RenjinBuildHistoryGroup(RenjinBuildHistory history) {
    this.versions = Collections.singletonList(history);
    this.visible = true;
  }


  public RenjinBuildHistoryGroup(List<RenjinBuildHistory> histories) {
    this.versions = histories;
    this.visible = false;
  }

  public List<RenjinBuildHistory> getVersions() {
    return versions;
  }

  public String getRange() {
    String first = versions.get(0).getLabel();
    String last = versions.get(versions.size() - 1).getLabel();
    return first + "..." + last;
  }

  public String getHiddenStyle() {
    if(visible) {
      return "";
    } else {
      return "style=\"display: none\"";
    }
  }

  public boolean isVisible() {
    return visible;
  }
}
