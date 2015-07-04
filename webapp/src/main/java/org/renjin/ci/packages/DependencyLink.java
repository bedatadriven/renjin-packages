package org.renjin.ci.packages;


import org.renjin.ci.model.PackageVersionId;

public class DependencyLink {
  private final String label;
  private final String url;
  private final boolean successful;

  public DependencyLink(PackageVersionId packageVersionId, boolean successful) {
    this.url = packageVersionId.getPath();
    this.label = packageVersionId.getPackageName() + " " + packageVersionId.getVersion();
    this.successful = successful;
  }

  public DependencyLink(String versionString, boolean successful) {
    this.url = "#";
    this.label = versionString;
    this.successful = successful;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }
  
  public String getStyle() {
    if(successful) {
      return "btn btn-successful";
    } else {
      return "btn btn-danger";
    }
  }
}
