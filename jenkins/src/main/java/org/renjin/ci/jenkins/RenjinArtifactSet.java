package org.renjin.ci.jenkins;

import hudson.model.Action;

import java.io.Serializable;
import java.util.List;

public class RenjinArtifactSet implements Action, Serializable {

  private List<String> paths;

  public RenjinArtifactSet(List<String> paths) {
    this.paths = paths;
  }

  @Override
  public String getIconFileName() {
    return "package.gif";
  }

  @Override
  public String getDisplayName() {
    return "Renjin Artifacts";
  }

  @Override
  public String getUrlName() {
    return null;
  }

  public List<String> getPaths() {
    return paths;
  }
}
