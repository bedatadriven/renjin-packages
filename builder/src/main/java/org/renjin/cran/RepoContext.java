package org.renjin.cran;


public class RepoContext {
  private boolean offline;
  private boolean updateSnapshots;
  private String renjinVersion;

  public boolean isOffline() {
    return offline;
  }

  public void setOffline(boolean offline) {
    this.offline = offline;
  }

  public boolean isUpdateSnapshots() {
    return updateSnapshots;
  }

  public void setUpdateSnapshots(boolean updateSnapshots) {
    this.updateSnapshots = updateSnapshots;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }
}
